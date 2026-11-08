package common;

/**
 * Something a user may be allowed to do. Each Role holds the set of permissions it has
 * (see {@link Role#can(Permission)}), so features check a permission instead of comparing role names.
 */
public enum Permission {
    /** See the inventory of your own branch. */
    VIEW_INVENTORY,
    /** Sell products from your branch's inventory (the stock goes down). */
    SELL,
    /** Buy new stock for your branch (the stock goes up). */
    RESTOCK,
    /** See the inventory of every branch, not only your own. */
    VIEW_ALL_BRANCHES
}
