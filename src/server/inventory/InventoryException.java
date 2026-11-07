package server.inventory;

/**
 * Something the user asked for cannot be done in the inventory (no permission, not enough stock, ...).
 * The message is written for the user, so the server can show it as it is.
 *
 * Unchecked (a RuntimeException) on purpose: these are business-rule refusals that the code which
 * talks to the client catches in one place, not errors every method in between must declare.
 */
public class InventoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message what went wrong, in words the user understands */
    public InventoryException(String message) {
        super(message);
    }

    /**
     * @param message what went wrong, in words the user understands
     * @param cause   the lower-level error behind it
     */
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
