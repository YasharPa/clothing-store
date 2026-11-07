package server.inventory;

import common.Product;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Where the products and the branches' inventories are kept.
 * The InventoryService only knows this interface, so the storage (memory for tests, files for the
 * real server, maybe a database one day) can change without changing the service.
 */
public interface InventoryRepository {

    /** @return every product in the catalog, sorted by product id */
    List<Product> findAllProducts();

    /** @return the product with this id, or empty if there is none */
    Optional<Product> findProduct(int productId);

    /**
     * Returns the inventory of a branch. Never null: a branch that has no stock yet gets a new,
     * empty inventory, which the repository remembers.
     * The same branch always gives back the SAME object, so everyone locks the same object.
     */
    BranchInventory findByBranch(String branch);

    /** @return the names of all branches that have an inventory, sorted */
    List<String> findAllBranches();

    /**
     * Stores the current state of a branch's inventory (call it after every change).
     *
     * @throws IOException if it could not be stored
     */
    void save(BranchInventory inventory) throws IOException;
}
