package common;

import java.io.Serializable;

/**
 * The news that a product's stock in a branch changed (after a successful sale or purchase).
 * The InventoryService hands it to every listener of that branch (Observer pattern).
 * It is Serializable so that later the server can send it to every client looking at the branch,
 * which is how all the employees of the branch see the change at once.
 *
 * @param branch      the branch whose stock changed
 * @param productId   the product whose stock changed
 * @param newQuantity how many units the branch has now
 */
public record InventoryUpdate(String branch, int productId, int newQuantity) implements Serializable {

    private static final long serialVersionUID = 1L;
}
