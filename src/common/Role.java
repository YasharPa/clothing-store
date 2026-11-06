package common;

/**
 * The level of a user in the system.
 * Customers are not users: they are records that employees manage.
 */
public enum Role {
    /** Creates and manages employee accounts. */
    ADMIN,
    /** Manages a shift; can also join chats. */
    SHIFT_MANAGER,
    CASHIER,
    SALESPERSON
}
