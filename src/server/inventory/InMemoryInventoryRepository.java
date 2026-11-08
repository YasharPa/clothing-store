package server.inventory;

import common.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A repository that keeps everything in memory only (lost when the program stops).
 * Used by the tests, and inside FileInventoryRepository, which also writes everything to files.
 */
public class InMemoryInventoryRepository implements InventoryRepository {

    // Thread-safe maps: many client threads use the repository at the same time.
    private final Map<Integer, Product> products = new ConcurrentHashMap<>();
    private final Map<String, BranchInventory> branches = new ConcurrentHashMap<>();

    /**
     * Adds a product to the catalog, or replaces the product with the same id.
     *
     * @throws IllegalArgumentException if product is null
     */
    public void addProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("The product must not be null");
        }
        products.put(product.getId(), product);
    }

    /** Removes a product from the catalog. Does nothing if there is no such product. */
    public void removeProduct(int productId) {
        products.remove(productId);
    }

    @Override
    public List<Product> findAllProducts() {
        List<Product> sorted = new ArrayList<>(products.values());
        sorted.sort((first, second) -> Integer.compare(first.getId(), second.getId()));
        return sorted;
    }

    @Override
    public Optional<Product> findProduct(int productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public BranchInventory findByBranch(String branch) {
        // computeIfAbsent checks and creates in ONE step, so two threads asking for a new branch
        // at the same moment still get the same object.
        return branches.computeIfAbsent(branch, BranchInventory::new);
    }

    @Override
    public List<String> findAllBranches() {
        List<String> names = new ArrayList<>(branches.keySet());
        names.sort(null); // null = the natural (alphabetical) order
        return names;
    }

    /** Nothing to do: the BranchInventory objects ARE the stored data. */
    @Override
    public void save(BranchInventory inventory) {
        // Intentionally empty.
    }
}
