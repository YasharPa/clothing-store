package server;

import common.Customer;
import common.CustomerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the data is kept in text files and is still there after the program starts again. */
class DatabaseTest {

    @TempDir
    Path folder; // JUnit gives each test its own empty temporary folder

    @Test
    void anEmptyFolderGetsSampleDataAndFiles() throws IOException {
        Database db = new Database(folder);

        assertFalse(db.store().branches.isEmpty());
        assertFalse(db.store().products.isEmpty());
        assertFalse(db.store().customers.isEmpty());
        assertTrue(Files.exists(folder.resolve("customers.txt")));
    }

    @Test
    void aChangedCustomerIsStillThereAfterOpeningAgain() throws IOException {
        Database db = new Database(folder);
        Customer customer = Customer.create(CustomerType.RETURNING, "300000001", "Dana Levi", "0501234567", 4, 820.5);
        db.store().customers.put("300000001", customer);
        db.save();

        Database again = new Database(folder);

        Customer loaded = again.store().customers.get("300000001");
        assertEquals(CustomerType.RETURNING, loaded.getType());
        assertEquals("Dana Levi", loaded.getFullName());
        assertEquals(4, loaded.getPurchaseCount());
        assertEquals(820.5, loaded.getTotalSpent(), 0.001);
    }

    @Test
    void aSaleIsStillThereAfterOpeningAgain() throws IOException {
        Database db = new Database(folder);
        int before = db.store().sales.size();
        db.addSale(1, 1, 2, 150.0, LocalDateTime.of(2026, 1, 5, 10, 0));

        Database again = new Database(folder);

        assertEquals(before + 1, again.store().sales.size());
        assertEquals(150.0, again.store().sales.get(before).amount(), 0.001);
    }

    @Test
    void theNoArgumentDatabaseHasASmallFixedDataSet() {
        Database db = new Database();

        assertEquals(2, db.store().branches.size());
        assertEquals(3, db.store().products.size());
        assertEquals(3, db.store().customers.size());
    }
}
