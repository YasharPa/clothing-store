package server.inventory;

/** There is no product with this number in the catalog. */
public class UnknownProductException extends InventoryException {

    private static final long serialVersionUID = 1L;

    private final int productId;

    /** @param productId the product number that does not exist */
    public UnknownProductException(int productId) {
        super("There is no product #" + productId);
        this.productId = productId;
    }

    public int getProductId() {
        return productId;
    }
}
