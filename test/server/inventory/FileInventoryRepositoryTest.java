package server.inventory;

import common.Category;
import common.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileInventoryRepositoryTest {

    @TempDir
    Path folder;

    private FileInventoryRepository open() throws IOException {
        return new FileInventoryRepository(folder.resolve("products.txt"), folder.resolve("inventory.txt"));
    }

    @Test
    void startsEmptyWhenThereAreNoFiles() throws IOException {
        FileInventoryRepository repository = open();

        assertTrue(repository.findAllProducts().isEmpty());
        assertTrue(repository.findAllBranches().isEmpty());
    }

    @Test
    void productsAndStockSurviveARestart() throws IOException {
        FileInventoryRepository first = open();
        first.addProduct(new Product(2, "Jeans", Category.PANTS, 120.5));
        first.addProduct(new Product(1, "Shirt", Category.SHIRTS, 50));
        BranchInventory telAviv = first.findByBranch("Tel Aviv");
        telAviv.addStock(1, 5);
        telAviv.addStock(2, 3);
        first.save(telAviv);
        BranchInventory haifa = first.findByBranch("Haifa");
        haifa.addStock(1, 7);
        first.save(haifa);

        FileInventoryRepository second = open(); // like restarting the server

        assertEquals(List.of(new Product(1, "Shirt", Category.SHIRTS, 50), new Product(2, "Jeans", Category.PANTS, 120.5)),
                second.findAllProducts());
        assertEquals(120.5, second.findProduct(2).orElseThrow().getPrice());
        assertEquals(5, second.findByBranch("Tel Aviv").getQuantity(1));
        assertEquals(3, second.findByBranch("Tel Aviv").getQuantity(2));
        assertEquals(7, second.findByBranch("Haifa").getQuantity(1));
        assertEquals(List.of("Haifa", "Tel Aviv"), second.findAllBranches());
    }

    @Test
    void aSoldOutProductStaysInTheFileWithZero() throws IOException {
        FileInventoryRepository first = open();
        first.addProduct(new Product(1, "Shirt", Category.SHIRTS, 50));
        BranchInventory telAviv = first.findByBranch("Tel Aviv");
        telAviv.addStock(1, 2);
        telAviv.removeStock(1, 2);
        first.save(telAviv);

        assertEquals(List.of("Tel Aviv;1;0"), Files.readAllLines(folder.resolve("inventory.txt")));
        assertEquals(0, open().findByBranch("Tel Aviv").getQuantity(1));
    }

    @Test
    void aBadLineInAFileIsReported() throws IOException {
        Files.writeString(folder.resolve("products.txt"), "1;Shirt;NOT_A_CATEGORY;50\n");

        assertThrows(IOException.class, this::open);
    }

    @Test
    void aNameWithTheSeparatorIsRefused() throws IOException {
        FileInventoryRepository repository = open();

        assertThrows(IllegalArgumentException.class,
                () -> repository.addProduct(new Product(1, "Shirt;Blue", Category.SHIRTS, 50)));
        assertTrue(repository.findAllProducts().isEmpty());
    }

    @Test
    void worksWithTheInventoryService() throws IOException {
        FileInventoryRepository repository = open();
        repository.addProduct(new Product(1, "Shirt", Category.SHIRTS, 50));
        InventoryService service = new InventoryService(repository);
        common.User manager = new common.User("moshe", common.Role.SHIFT_MANAGER, "Tel Aviv");

        service.restock(manager, 1, 10);
        service.sell(manager, 1, 4);

        assertEquals(6, open().findByBranch("Tel Aviv").getQuantity(1));
    }
}
