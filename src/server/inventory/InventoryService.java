package server.inventory;

import common.InventoryUpdate;
import common.Permission;
import common.Product;
import common.StockItem;
import common.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * All the inventory rules in one place (requirements 4 and 5):
 * <ul>
 *   <li>every action first checks the user's permission;</li>
 *   <li>sales and purchases always work on the LOGGED-IN USER'S branch: there is no branch parameter,
 *       so nobody can change another branch's stock;</li>
 *   <li>after every successful change, everyone listening to that branch is told (Observer pattern).</li>
 * </ul>
 * One InventoryService is shared by all the client threads, and it is thread-safe.
 */
public class InventoryService {

    private final InventoryRepository repository;
    // branch -> its listeners. CopyOnWriteArrayList: we can go over the list while another thread adds or removes.
    private final Map<String, List<InventoryListener>> listeners = new ConcurrentHashMap<>();

    /**
     * @param repository where the products and inventories are kept (dependency injection:
     *                   the service gets it from outside, so tests can pass an in-memory one)
     */
    public InventoryService(InventoryRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * The branches this user may look at: every branch for a role with VIEW_ALL_BRANCHES,
     * otherwise only the user's own branch.
     *
     * @throws PermissionDeniedException if the user may not see inventory at all
     */
    public List<String> getBranches(User user) {
        require(user, Permission.VIEW_INVENTORY);
        if (!user.getRole().can(Permission.VIEW_ALL_BRANCHES)) {
            return List.of(user.getBranch());
        }
        List<String> branches = new ArrayList<>(repository.findAllBranches());
        if (!branches.contains(user.getBranch())) {
            branches.add(user.getBranch());
        }
        return branches;
    }

    /**
     * The inventory of the user's own branch.
     *
     * @return one StockItem per product in the catalog (quantity 0 if the branch has none of it)
     * @throws PermissionDeniedException if the user may not see inventory
     */
    public List<StockItem> getInventory(User user) {
        return getInventory(user, user.getBranch());
    }

    /**
     * The inventory of a given branch. Looking at a branch other than your own needs VIEW_ALL_BRANCHES.
     *
     * @return one StockItem per product in the catalog (quantity 0 if the branch has none of it)
     * @throws PermissionDeniedException if the user may not see this branch
     */
    public List<StockItem> getInventory(User user, String branch) {
        require(user, Permission.VIEW_INVENTORY);
        if (!user.getBranch().equals(branch)) {
            require(user, Permission.VIEW_ALL_BRANCHES);
        }
        Map<Integer, Integer> quantities = repository.findByBranch(branch).getAll();
        List<StockItem> items = new ArrayList<>();
        for (Product product : repository.findAllProducts()) {
            items.add(new StockItem(product, quantities.getOrDefault(product.getId(), 0)));
        }
        return items;
    }

    /**
     * Sells units of a product from the user's own branch.
     *
     * @return how many units are left
     * @throws PermissionDeniedException   if the user's role may not sell
     * @throws UnknownProductException     if there is no such product
     * @throws IllegalArgumentException    if quantity is not greater than 0
     * @throws InsufficientStockException if the branch has fewer units; nothing changes then
     * @throws InventoryException          if the change could not be saved; nothing changes then
     */
    public int sell(User user, int productId, int quantity) {
        require(user, Permission.SELL);
        checkProductExists(productId);
        BranchInventory inventory = repository.findByBranch(user.getBranch());

        int left;
        // Lock only THIS branch: a sale in another branch does not wait for us.
        // Changing and saving happen together, so the file always matches memory.
        synchronized (inventory) {
            left = inventory.removeStock(productId, quantity);
            saveOrUndo(inventory, () -> inventory.addStock(productId, quantity));
        }
        // TODO (requirement 6): price by customer type, e.g. customer.applyPromotion(product.getPrice() * quantity)
        // TODO (requirements 7 and 10): store a SalesRecord and write a SALE log line
        notifyListeners(new InventoryUpdate(user.getBranch(), productId, left));
        return left;
    }

    /**
     * Buys new stock of a product for the user's own branch.
     *
     * @return how many units the branch has now
     * @throws PermissionDeniedException if the user's role may not buy stock
     * @throws UnknownProductException   if there is no such product
     * @throws IllegalArgumentException  if quantity is not greater than 0
     * @throws InventoryException        if the change could not be saved; nothing changes then
     */
    public int restock(User user, int productId, int quantity) {
        require(user, Permission.RESTOCK);
        checkProductExists(productId);
        BranchInventory inventory = repository.findByBranch(user.getBranch());

        int now;
        synchronized (inventory) {
            now = inventory.addStock(productId, quantity);
            saveOrUndo(inventory, () -> inventory.removeStock(productId, quantity));
        }
        // TODO (requirement 10): write a STOCK_PURCHASE log line
        notifyListeners(new InventoryUpdate(user.getBranch(), productId, now));
        return now;
    }

    /** Starts telling this listener about every stock change in the branch. */
    public void addListener(String branch, InventoryListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.computeIfAbsent(branch, name -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** Stops telling this listener about the branch. Does nothing if it was not registered. */
    public void removeListener(String branch, InventoryListener listener) {
        List<InventoryListener> branchListeners = listeners.get(branch);
        if (branchListeners != null) {
            branchListeners.remove(listener);
        }
    }

    /** Tells only the listeners of the update's branch. A broken listener does not stop the others. */
    private void notifyListeners(InventoryUpdate update) {
        for (InventoryListener listener : listeners.getOrDefault(update.branch(), List.of())) {
            try {
                listener.onInventoryChanged(update);
            } catch (RuntimeException e) {
                System.err.println("An inventory listener failed: " + e);
            }
        }
    }

    /**
     * Saves the branch. If saving fails, undoes the change in memory, so memory and storage stay the same.
     * The caller must hold the branch's lock, so nobody else changes the branch in between.
     */
    private void saveOrUndo(BranchInventory inventory, Runnable undo) {
        try {
            repository.save(inventory);
        } catch (IOException e) {
            undo.run();
            System.err.println("Could not save the inventory of " + inventory.getBranch() + ": " + e);
            throw new InventoryException("The server could not save the change, so nothing was changed", e);
        }
    }

    private void checkProductExists(int productId) {
        if (repository.findProduct(productId).isEmpty()) {
            throw new UnknownProductException(productId);
        }
    }

    private static void require(User user, Permission permission) {
        Objects.requireNonNull(user, "user");
        if (!user.getRole().can(permission)) {
            throw new PermissionDeniedException(user.getRole(), permission);
        }
    }
}
