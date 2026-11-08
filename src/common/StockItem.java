package common;

import java.io.Serializable;

/**
 * One row of a branch's inventory, as the server sends it to the client: a product and how many the branch has.
 *
 * @param product  the product
 * @param quantity how many units the branch has right now (0 or more)
 */
public record StockItem(Product product, int quantity) implements Serializable {

    private static final long serialVersionUID = 1L;
}
