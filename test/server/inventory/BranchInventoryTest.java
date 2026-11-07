package server.inventory;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BranchInventoryTest {

    private final BranchInventory inventory = new BranchInventory("Tel Aviv");

    @Test
    void aProductItNeverHadHasZeroUnits() {
        assertEquals(0, inventory.getQuantity(999));
    }

    @Test
    void addingAndRemovingChangeTheQuantity() {
        assertEquals(5, inventory.addStock(1, 5));
        assertEquals(8, inventory.addStock(1, 3));
        assertEquals(6, inventory.removeStock(1, 2));
        assertEquals(6, inventory.getQuantity(1));
    }

    @Test
    void removingMoreThanThereIsFailsAndChangesNothing() {
        inventory.addStock(1, 3);

        InsufficientStockException error = assertThrows(InsufficientStockException.class,
                () -> inventory.removeStock(1, 5));

        assertEquals(3, inventory.getQuantity(1));
        assertEquals(5, error.getRequested());
        assertEquals(3, error.getAvailable());
    }

    @Test
    void zeroOrNegativeQuantitiesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> inventory.addStock(1, 0));
        assertThrows(IllegalArgumentException.class, () -> inventory.addStock(1, -2));
        assertThrows(IllegalArgumentException.class, () -> inventory.removeStock(1, 0));
    }

    @Test
    void getAllIsAReadOnlyCopy() {
        inventory.addStock(1, 5);
        Map<Integer, Integer> all = inventory.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put(2, 100));

        inventory.addStock(1, 1);
        assertEquals(5, all.get(1)); // the copy did not change
    }

    @Test
    void aBranchNeedsAName() {
        assertThrows(IllegalArgumentException.class, () -> new BranchInventory(""));
        assertThrows(IllegalArgumentException.class, () -> new BranchInventory(null));
    }
}
