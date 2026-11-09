package server;

import common.*;
import common.DataTransfer.*;
import common.Product;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.inventory.InMemoryInventoryRepository;
import server.inventory.InventoryService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the requests that connect the inventory (part 2) to the customers, reports and log (part 3). */
class PartThreeHandlerTest {

    private final User manager = new User("moshe", Role.SHIFT_MANAGER, "Tel Aviv");
    private final User cashier = new User("dana", Role.CASHIER, "Tel Aviv");
    private final User seller = new User("avi", Role.SALESPERSON, "Tel Aviv");

    private PartThreeHandler handler;
    private Database db;

    @BeforeAll
    static void useATemporaryDataFolder() {
        TestEnv.init();
    }

    @BeforeEach
    void startWithOneProductAndTenInStock() {
        InMemoryInventoryRepository repository = new InMemoryInventoryRepository();
        repository.addProduct(new Product(1, "Polo Shirt", Category.SHIRTS, 100.0));
        repository.findByBranch("Tel Aviv").addStock(1, 10);
        db = new Database();
        handler = new PartThreeHandler(db, repository, new InventoryService(repository));
    }

    private Response send(User user, RequestType type, java.io.Serializable payload) {
        return handler.handle(user, new Request(type, payload));
    }

    @Test
    void aSaleLowersTheStockAndIsPricedForTheCustomer() {
        // 200000002 is a returning customer: 5% off
        Response response = send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 2, "200000002"));

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("To pay: 190.00"));
        assertTrue(response.getMessage().contains("Left in stock: 8"));
    }

    @Test
    void aSaleCountsAsAPurchaseOfTheCustomer() {
        send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 1, "200000002"));

        Customer customer = db.store().customers.get("200000002");
        assertEquals(4, customer.getPurchaseCount());
    }

    @Test
    void aSaleToACustomerThatDoesNotExistChangesNothing() {
        Response response = send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 1, "999999999"));

        assertFalse(response.isSuccess());
        Response inventory = send(cashier, RequestType.GET_INVENTORY, null);
        StockItem item = ((ArrayList<StockItem>) inventory.getData()).get(0);
        assertEquals(10, item.quantity());
    }

    @Test
    void aSaleWithoutACustomerIsFullPrice() {
        Response response = send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 1, null));

        assertTrue(response.getMessage().contains("To pay: 100.00"));
    }

    @Test
    void aSalePermissionIsCheckedByTheInventory() {
        Response response = send(seller, RequestType.SELL_ITEM, new SaleForm(1, 1, null));

        assertFalse(response.isSuccess());
    }

    @Test
    void aSaleAppearsInTheReport() {
        send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 3, null));

        ReportRequest request = new ReportRequest(ReportMode.BY_BRANCH, 0, null, new ArrayList<>(), new ArrayList<>());
        Response response = send(manager, RequestType.GET_REPORT, request);

        String text = ((ReportResult) response.getData()).asText();
        assertTrue(text.contains("Branch Tel Aviv: 1 sales, 3 items, revenue 300.00"));
    }

    @Test
    void aSaleIsWrittenToTheLog() {
        send(cashier, RequestType.SELL_ITEM, new SaleForm(1, 1, null));

        Response response = send(manager, RequestType.GET_LOGS, new LogQuery(LogType.SALE, 1000));

        List<LogEntry> entries = (ArrayList<LogEntry>) response.getData();
        assertEquals("dana", entries.get(entries.size() - 1).user());
    }

    @Test
    void aShiftManagerCanBuyStockAndItIsLogged() {
        Response response = send(manager, RequestType.RESTOCK_ITEM, new StockChange(1, 5));

        assertTrue(response.getMessage().contains("In stock now: 15"));
        Response logs = send(manager, RequestType.GET_LOGS, new LogQuery(LogType.STOCK_PURCHASE, 1000));
        assertFalse(((ArrayList<LogEntry>) logs.getData()).isEmpty());
    }

    @Test
    void aCashierCannotBuyStock() {
        Response response = send(cashier, RequestType.RESTOCK_ITEM, new StockChange(1, 5));

        assertFalse(response.isSuccess());
    }

    @Test
    void anUnknownProductIsRefused() {
        Response response = send(cashier, RequestType.SELL_ITEM, new SaleForm(77, 1, null));

        assertFalse(response.isSuccess());
    }

    @Test
    void theBranchesAreListedWithTheirNumbers() {
        Response response = send(manager, RequestType.GET_BRANCHES, null);

        ArrayList<String> branches = (ArrayList<String>) response.getData();
        assertTrue(branches.contains("1 - Tel Aviv"));
    }

    @Test
    void nobodyLoggedInGetsAnError() {
        Response response = send(null, RequestType.GET_INVENTORY, null);

        assertFalse(response.isSuccess());
    }
}
