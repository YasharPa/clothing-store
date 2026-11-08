package common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the permission table of the roles. */
class RoleTest {

    @Test
    void everyRoleCanSeeTheInventoryOfItsBranch() {
        for (Role role : Role.values()) {
            assertTrue(role.can(Permission.VIEW_INVENTORY), role + " should see the inventory");
        }
    }

    @Test
    void shiftManagerCanSellAndBuyStock() {
        assertTrue(Role.SHIFT_MANAGER.can(Permission.SELL));
        assertTrue(Role.SHIFT_MANAGER.can(Permission.RESTOCK));
        assertFalse(Role.SHIFT_MANAGER.can(Permission.VIEW_ALL_BRANCHES));
    }

    @Test
    void cashierCanSellButNotBuyStock() {
        assertTrue(Role.CASHIER.can(Permission.SELL));
        assertFalse(Role.CASHIER.can(Permission.RESTOCK));
    }

    @Test
    void salespersonCanOnlyLook() {
        assertFalse(Role.SALESPERSON.can(Permission.SELL));
        assertFalse(Role.SALESPERSON.can(Permission.RESTOCK));
        assertFalse(Role.SALESPERSON.can(Permission.VIEW_ALL_BRANCHES));
    }

    @Test
    void onlyTheAdminSeesAllBranchesAndTheAdminDoesNotSell() {
        assertTrue(Role.ADMIN.can(Permission.VIEW_ALL_BRANCHES));
        assertFalse(Role.ADMIN.can(Permission.SELL));
        assertFalse(Role.ADMIN.can(Permission.RESTOCK));
    }
}
