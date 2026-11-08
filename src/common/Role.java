package common;

import java.util.EnumSet;
import java.util.Set;

/**
 * The level of a user in the system, and what that level is allowed to do.
 * Customers are not users: they are records that employees manage.
 *
 * To change who may do what, change only the permission lists below;
 * the features ask {@link #can(Permission)} and never compare role names.
 */
public enum Role {
    /** Creates and manages employee accounts. Can look at every branch's inventory, but does not sell or buy. */
    ADMIN(EnumSet.of(Permission.VIEW_INVENTORY, Permission.VIEW_ALL_BRANCHES)),
    /** Manages a shift; can also join chats. Sells and buys stock for their branch. */
    SHIFT_MANAGER(EnumSet.of(Permission.VIEW_INVENTORY, Permission.SELL, Permission.RESTOCK)),
    /** Sells products in their branch. */
    CASHIER(EnumSet.of(Permission.VIEW_INVENTORY, Permission.SELL)),
    /** Helps customers on the floor: sees what is in stock in their branch. */
    SALESPERSON(EnumSet.of(Permission.VIEW_INVENTORY));

    private final Set<Permission> permissions;

    /** @param permissions everything this role may do */
    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Checks whether this role has the given permission.
     *
     * @param permission the permission to check
     * @return true if the role has the permission
     */
    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }
}
