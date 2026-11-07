package server.inventory;

/** Someone tried to sell more units than the branch has. Nothing was changed. */
public class InsufficientStockException extends InventoryException {

    private static final long serialVersionUID = 1L;

    private final int productId;
    private final int requested;
    private final int available;

    /**
     * @param productId the product that was asked for
     * @param requested how many units were asked for
     * @param available how many units the branch has
     */
    public InsufficientStockException(int productId, int requested, int available) {
        super("Not enough stock of product #" + productId + ": asked for " + requested + ", only " + available + " left");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public int getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
