package server.inventory;

import common.InventoryUpdate;

/**
 * Observer pattern: something that wants to hear about every stock change in a branch.
 * Register it with {@link InventoryService#addListener(String, InventoryListener)}.
 * Later the server's ClientHandler will implement it and pass each update to its client.
 */
@FunctionalInterface
public interface InventoryListener {

    /**
     * Called after a sale or purchase in the branch succeeded.
     * Runs on the thread of the employee who made the change, so it must be quick.
     */
    void onInventoryChanged(InventoryUpdate update);
}
