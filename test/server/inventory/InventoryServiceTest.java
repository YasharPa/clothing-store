package server.inventory;

import common.Category;
import common.InventoryUpdate;
import common.Product;
import common.Role;
import common.StockItem;
import common.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The scenarios of requirements 4 and 5: permissions, own branch only, sales, purchases and updates. */
class InventoryServiceTest {

    private static final int SHIRT = 1;
    private static final int JEANS = 2;

    private final User cashierTelAviv = new User("dana", Role.CASHIER, "Tel Aviv");
    private final User managerTelAviv = new User("moshe", Role.SHIFT_MANAGER, "Tel Aviv");
    private final User managerHaifa = new User("rina", Role.SHIFT_MANAGER, "Haifa");
    private final User sellerHaifa = new User("eli", Role.SALESPERSON, "Haifa");
    private final User admin = new User("admin", Role.ADMIN, "HQ");

    private InMemoryInventoryRepository repository;
    private InventoryService service;

    @BeforeEach
    void createShopWithTwoBranches() {
        repository = new InMemoryInventoryRepository();
        repository.addProduct(new Product(SHIRT, "Shirt", Category.SHIRTS, 50));
        repository.addProduct(new Product(JEANS, "Jeans", Category.PANTS, 120));
        repository.findByBranch("Tel Aviv").addStock(SHIRT, 5);
        repository.findByBranch("Haifa").addStock(SHIRT, 5);
        service = new InventoryService(repository);
    }

    private int quantity(String branch, int productId) {
        return repository.findByBranch(branch).getQuantity(productId);
    }

    // ---- permissions ----

    @Test
    void aSalespersonCannotSell() {
        assertThrows(PermissionDeniedException.class, () -> service.sell(sellerHaifa, SHIRT, 1));
        assertEquals(5, quantity("Haifa", SHIRT));
    }

    @Test
    void aCashierCannotBuyStock() {
        assertThrows(PermissionDeniedException.class, () -> service.restock(cashierTelAviv, SHIRT, 10));
        assertEquals(5, quantity("Tel Aviv", SHIRT));
    }

    // ---- requirement 4: only your own branch ----

    @Test
    void aSaleChangesOnlyTheSellersBranch() {
        int left = service.sell(cashierTelAviv, SHIRT, 2);

        assertEquals(3, left);
        assertEquals(3, quantity("Tel Aviv", SHIRT));
        assertEquals(5, quantity("Haifa", SHIRT));
    }

    @Test
    void aPurchaseChangesOnlyTheBuyersBranch() {
        service.restock(managerHaifa, SHIRT, 10);

        assertEquals(15, quantity("Haifa", SHIRT));
        assertEquals(5, quantity("Tel Aviv", SHIRT));
    }

    @Test
    void anEmployeeSeesTheInventoryOfTheirOwnBranch() {
        service.restock(managerHaifa, JEANS, 4);

        List<StockItem> items = service.getInventory(sellerHaifa);

        assertEquals(2, items.size()); // every product of the catalog
        assertEquals(new StockItem(new Product(SHIRT, "Shirt", Category.SHIRTS, 50), 5), items.get(0));
        assertEquals(4, items.get(1).quantity());
    }

    @Test
    void anEmployeeCannotLookAtAnotherBranch() {
        assertThrows(PermissionDeniedException.class, () -> service.getInventory(sellerHaifa, "Tel Aviv"));
        assertEquals(List.of("Haifa"), service.getBranches(sellerHaifa));
    }

    @Test
    void theAdminCanLookAtEveryBranch() {
        service.sell(cashierTelAviv, SHIRT, 1);

        assertEquals(4, service.getInventory(admin, "Tel Aviv").get(0).quantity());
        assertTrue(service.getBranches(admin).containsAll(List.of("Tel Aviv", "Haifa", "HQ")));
    }

    // ---- requirement 5: selling and buying ----

    @Test
    void sellingMoreThanThereIsFailsAndChangesNothing() {
        assertThrows(InsufficientStockException.class, () -> service.sell(cashierTelAviv, SHIRT, 6));
        assertEquals(5, quantity("Tel Aviv", SHIRT));
    }

    @Test
    void anUnknownProductIsRefused() {
        assertThrows(UnknownProductException.class, () -> service.sell(cashierTelAviv, 999, 1));
        assertThrows(UnknownProductException.class, () -> service.restock(managerTelAviv, 999, 1));
    }

    @Test
    void zeroOrNegativeQuantitiesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.sell(cashierTelAviv, SHIRT, 0));
        assertThrows(IllegalArgumentException.class, () -> service.restock(managerTelAviv, SHIRT, -3));
        assertEquals(5, quantity("Tel Aviv", SHIRT));
    }

    /** A storage whose disk is always full: reads work, every save fails. */
    private static final class BrokenStorage implements InventoryRepository {
        private final InventoryRepository data;

        BrokenStorage(InventoryRepository data) {
            this.data = data;
        }

        @Override
        public List<Product> findAllProducts() {
            return data.findAllProducts();
        }

        @Override
        public java.util.Optional<Product> findProduct(int productId) {
            return data.findProduct(productId);
        }

        @Override
        public BranchInventory findByBranch(String branch) {
            return data.findByBranch(branch);
        }

        @Override
        public List<String> findAllBranches() {
            return data.findAllBranches();
        }

        @Override
        public void save(BranchInventory inventory) throws IOException {
            throw new IOException("disk full");
        }
    }

    @Test
    void ifSavingFailsTheChangeIsUndone() {
        InventoryService serviceWithBrokenStorage = new InventoryService(new BrokenStorage(repository));
        List<InventoryUpdate> heard = new ArrayList<>();
        serviceWithBrokenStorage.addListener("Tel Aviv", heard::add);

        assertThrows(InventoryException.class, () -> serviceWithBrokenStorage.sell(cashierTelAviv, SHIRT, 2));
        assertThrows(InventoryException.class, () -> serviceWithBrokenStorage.restock(managerTelAviv, SHIRT, 2));

        assertEquals(5, quantity("Tel Aviv", SHIRT));
        assertTrue(heard.isEmpty());
    }

    // ---- Observer: everyone in the branch hears about the change ----

    @Test
    void onlyTheListenersOfTheBranchAreTold() {
        List<String> heard = new ArrayList<>();
        service.addListener("Tel Aviv", update -> heard.add("first: " + update.newQuantity()));
        service.addListener("Tel Aviv", update -> heard.add("second: " + update.newQuantity()));
        service.addListener("Haifa", update -> heard.add("haifa"));

        service.sell(cashierTelAviv, SHIRT, 2);

        assertEquals(List.of("first: 3", "second: 3"), heard);
    }

    @Test
    void theUpdateSaysWhatChanged() {
        List<InventoryUpdate> heard = new ArrayList<>();
        service.addListener("Haifa", heard::add);

        service.restock(managerHaifa, JEANS, 7);

        assertEquals(List.of(new InventoryUpdate("Haifa", JEANS, 7)), heard);
    }

    @Test
    void aFailedSaleTellsNobody() {
        List<InventoryUpdate> heard = new ArrayList<>();
        service.addListener("Tel Aviv", heard::add);

        assertThrows(InsufficientStockException.class, () -> service.sell(cashierTelAviv, SHIRT, 100));

        assertTrue(heard.isEmpty());
    }

    @Test
    void aRemovedListenerIsNotToldAnymore() {
        List<InventoryUpdate> heard = new ArrayList<>();
        InventoryListener listener = heard::add;
        service.addListener("Tel Aviv", listener);
        service.removeListener("Tel Aviv", listener);

        service.sell(cashierTelAviv, SHIRT, 1);

        assertTrue(heard.isEmpty());
    }

    @Test
    void aBrokenListenerDoesNotStopTheSaleOrTheOtherListeners() {
        List<InventoryUpdate> heard = new ArrayList<>();
        service.addListener("Tel Aviv", update -> {
            throw new IllegalStateException("this listener is broken");
        });
        service.addListener("Tel Aviv", heard::add);

        assertEquals(4, service.sell(cashierTelAviv, SHIRT, 1));
        assertEquals(1, heard.size());
    }
}
