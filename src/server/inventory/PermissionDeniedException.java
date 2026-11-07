package server.inventory;

import common.Permission;
import common.Role;

/** The user's role does not have the permission the action needs. */
public class PermissionDeniedException extends InventoryException {

    private static final long serialVersionUID = 1L;

    private final Role role;
    private final Permission permission;

    /**
     * @param role       the role of the user who tried
     * @param permission the permission that was missing
     */
    public PermissionDeniedException(Role role, Permission permission) {
        super("Not allowed: a " + role + " does not have the permission " + permission);
        this.role = role;
        this.permission = permission;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }
}
