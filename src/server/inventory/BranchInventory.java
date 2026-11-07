package server.inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * How many units of each product ONE branch has.
 *
 * Thread-safe: every method is synchronized on this object, so two cashiers of the same branch
 * cannot change it at the same moment, while different branches (different objects) never wait for each other.
 */
public class BranchInventory {

    private final String branch;
    // product id -> quantity. A product that is not in the map has 0 units.
    private final Map<Integer, Integer> quantities = new HashMap<>();

    /**
     * Creates an empty inventory.
     *
     * @param branch the branch's name, the same text as in User.getBranch() (not empty)
     */
    public BranchInventory(String branch) {
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("The branch name must not be empty");
        }
        this.branch = branch;
    }

    public String getBranch() {
        return branch;
    }

    /**
     * Adds units of a product (for example after buying new stock).
     *
     * @return the quantity after adding
     * @throws IllegalArgumentException if quantity is not greater than 0
     */
    public synchronized int addStock(int productId, int quantity) {
        checkPositive(quantity);
        // addExact throws an error instead of silently wrapping around to a negative number.
        int newQuantity = Math.addExact(getQuantity(productId), quantity);
        quantities.put(productId, newQuantity);
        return newQuantity;
    }

    /**
     * Removes units of a product (for example after a sale).
     * Checking "is there enough?" and removing happen in one synchronized step, so two sales
     * can never both take the last unit.
     *
     * @return the quantity that is left
     * @throws IllegalArgumentException    if quantity is not greater than 0
     * @throws InsufficientStockException if the branch has fewer units than asked for; nothing changes then
     */
    public synchronized int removeStock(int productId, int quantity) {
        checkPositive(quantity);
        int available = getQuantity(productId);
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }
        int left = available - quantity;
        quantities.put(productId, left);
        return left;
    }

    /** @return how many units of the product the branch has; 0 for a product it never had */
    public synchronized int getQuantity(int productId) {
        return quantities.getOrDefault(productId, 0);
    }

    /**
     * @return a read-only COPY of all quantities (product id -> quantity). Changing the branch later
     *         does not change the copy, and nobody can change the branch through it.
     */
    public synchronized Map<Integer, Integer> getAll() {
        return Map.copyOf(quantities);
    }

    private static void checkPositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("The quantity must be greater than 0");
        }
    }
}
