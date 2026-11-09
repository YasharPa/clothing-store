package server;

import common.*;
import common.DataTransfer.*;
import common.Product;
import server.inventory.FileInventoryRepository;
import server.inventory.InventoryException;
import server.inventory.InventoryRepository;
import server.inventory.InventoryService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles the requests of part: customers, reports and logs, and the requests that connect the inventory
 * to them (a sale records a SalesRecord for the reports, updates the customer and writes a log line).
 * ClientHandler passes these requests here, so all this code stays in this one file.
 * The data (Database, InventoryService) is shared by all clients and created once, on first use.
 */
public class PartThreeHandler {
    private static Database sharedDatabase;
    private static InventoryRepository sharedRepository;
    private static InventoryService sharedInventory;

    private final Database db;
    private final InventoryRepository repository;
    private final InventoryService inventory;
    private final CustomerService customerService;
    private final ReportService reportService;
    private final LogQueryService logQueryService = new LogQueryService();
    private final LogService log = LogService.getInstance();

    /** The normal handler: it uses the data that all the clients share. */
    public PartThreeHandler() {
        this(sharedDatabase(), sharedRepository, sharedInventory);
    }

    /** A handler on the given data (for the tests, which pass a temporary Database and an in-memory inventory). */
    public PartThreeHandler(Database db, InventoryRepository repository, InventoryService inventory) {
        this.db = db;
        this.repository = repository;
        this.inventory = inventory;
        this.customerService = new CustomerService(db);
        this.reportService = new ReportService(db);
        syncCatalog();
    }

    private static synchronized Database sharedDatabase() {
        if (sharedDatabase == null) {
            Path dir = Path.of(System.getProperty("data.dir", "data"));
            try {
                sharedDatabase = Database.getInstance();
                sharedRepository = new FileInventoryRepository(dir.resolve("inventory-products.txt"),
                        dir.resolve("inventory-stock.txt"));
            } catch (IOException e) {
                throw new IllegalStateException("Could not open the inventory files: " + e.getMessage(), e);
            }
            sharedInventory = new InventoryService(sharedRepository);
        }
        return sharedDatabase;
    }

    /**
     * @param user    who is logged in on this connection (null = nobody)
     * @param request one of the part 3 request types
     */
    public Response handle(User user, Request request) {
        if (user == null) {
            return new Response(false, "Please log in first");
        }
        try {
            return switch (request.getType()) {
                case GET_CUSTOMERS -> new Response(true, "Customers", new ArrayList<>(customerService.list()));
                case GET_CUSTOMER -> getCustomer(request);
                case ADD_CUSTOMER -> addCustomer(user, request);
                case UPDATE_CUSTOMER -> updateCustomer(user, request);
                case GET_REPORT -> getReport(user, request);
                case GET_LOGS -> getLogs(user, request);
                case GET_INVENTORY -> getInventory(user, request);
                case SELL_ITEM -> sellItem(user, request);
                case RESTOCK_ITEM -> restockItem(user, request);
                case GET_BRANCHES -> getBranches();
                default -> new Response(false, "Unknown request");
            };
        } catch (ServiceException | InventoryException | IllegalArgumentException e) {
            return new Response(false, e.getMessage());
        }
    }

    // ================================================================ customers, reports, logs

    private Response getCustomer(Request request) {
        if (!(request.getPayload() instanceof String id)) {
            return new Response(false, "A customer ID is needed");
        }
        Customer c = customerService.get(id);
        return new Response(true, "Customer found", c);
    }

    private Response addCustomer(User user, Request request) {
        if (!(request.getPayload() instanceof CustomerForm form)) {
            return new Response(false, "The customer details are needed");
        }
        Customer c = customerService.register(user, form.type(), form.id(), form.fullName(), form.phone());
        return new Response(true, "Customer registered: " + c);
    }

    private Response updateCustomer(User user, Request request) {
        if (!(request.getPayload() instanceof CustomerForm form)) {
            return new Response(false, "The customer details are needed");
        }
        Customer c = customerService.update(user, form.id(), form.fullName(), form.phone());
        return new Response(true, "Customer updated: " + c);
    }

    private Response getReport(User user, Request request) {
        if (!(request.getPayload() instanceof ReportRequest report)) {
            return new Response(false, "The report details are needed");
        }
        syncCatalog();
        return new Response(true, "Report ready", reportService.generate(user, report));
    }

    private Response getLogs(User user, Request request) {
        if (!(request.getPayload() instanceof LogQuery query)) {
            return new Response(false, "The log query is needed");
        }
        return new Response(true, "Log entries", new ArrayList<>(logQueryService.read(user, query)));
    }

    private Response getBranches() {
        syncCatalog();
        ArrayList<String> branches = new ArrayList<>();
        synchronized (db) {
            for (common.DataTransfer.Branch b : db.store().branches.values()) {
                branches.add(b.toString());
            }
        }
        return new Response(true, "Branches", branches);
    }

    // ================================================================ inventory

    private Response getInventory(User user, Request request) {
        List<StockItem> items;
        if (request.getPayload() instanceof String branch) {
            items = inventory.getInventory(user, branch);
        } else {
            items = inventory.getInventory(user);
        }
        return new Response(true, "Inventory of " + user.getBranch(), new ArrayList<>(items));
    }

    /**
     * A sale. The InventoryService checks the permission and lowers the stock. Then the sale gets its price
     * (with the customer's promotion), a SalesRecord for the reports, and a line in the log.
     */
    private Response sellItem(User user, Request request) {
        if (!(request.getPayload() instanceof SaleForm form)) {
            return new Response(false, "The sale details are needed");
        }
        Product product = findProduct(user, form.productId());
        String customerId = form.customerId();
        if (customerId != null && customerId.isBlank()) {
            customerId = null;
        }
        if (customerId != null) {
            customerService.get(customerId);
        }

        int left = inventory.sell(user, form.productId(), form.quantity());

        double total = product.getPrice() * form.quantity();
        double paid = total;
        String promotion = "no customer";
        synchronized (db) {
            if (customerId != null) {
                CustomerService.Purchase purchase = customerService.recordPurchase(customerId, total);
                paid = purchase.paid();
                promotion = purchase.description();
            }
            int branchId = branchIdOf(user.getBranch());
            db.addSale(new SalesRecord(branchId, product.getId(), product.getName(), product.getCategory().name(),
                    form.quantity(), paid, LocalDateTime.now(), user.getUsername(), customerId));
        }
        log.log(LogType.SALE, user.getUsername(), form.quantity() + " x " + product.getName()
                + ", total " + money(paid) + " (" + promotion + ")");
        return new Response(true, "Sold " + form.quantity() + " x " + product.getName() + ". To pay: "
                + money(paid) + " (" + promotion + "). Left in stock: " + left);
    }

    private Response restockItem(User user, Request request) {
        if (!(request.getPayload() instanceof StockChange change)) {
            return new Response(false, "The purchase details are needed");
        }
        Product product = findProduct(user, change.productId());
        int now = inventory.restock(user, change.productId(), change.quantity());
        log.log(LogType.STOCK_PURCHASE, user.getUsername(), change.quantity() + " x " + product.getName()
                + " for branch " + user.getBranch());
        return new Response(true, "Bought " + change.quantity() + " x " + product.getName()
                + ". In stock now: " + now);
    }

    private Product findProduct(User user, int productId) {
        for (StockItem item : inventory.getInventory(user)) {
            if (item.product().getId() == productId) {
                return item.product();
            }
        }
        throw new ServiceException("There is no product with the number " + productId);
    }

    // ================================================================ keeping the reports' data in line with the inventory

    /**
     * The inventory (written by the inventory part) is the source of the products and branches. This copies them
     * into the Database, which the reports read, so a report always shows the same names as the inventory.
     * Branches keep their numbers: a branch that is already known is not renumbered.
     */
    private void syncCatalog() {
        synchronized (db) {
            Database.Store store = db.store();
            store.products.clear();
            for (Product p : repository.findAllProducts()) {
                store.products.put(p.getId(), new common.DataTransfer.Product(p.getId(), p.getName(),
                        p.getCategory().name(), p.getPrice()));
            }
            for (String name : repository.findAllBranches()) {
                if (!repository.findByBranch(name).getAll().isEmpty()) {
                    branchIdOf(name);
                }
            }
            db.save();
        }
    }

    /** The number of the branch with this name. A new name gets the next free number. Hold the lock on db. */
    private int branchIdOf(String name) {
        int highest = 0;
        for (common.DataTransfer.Branch b : db.store().branches.values()) {
            if (b.name().equals(name)) {
                return b.id();
            }
            highest = Math.max(highest, b.id());
        }
        int id = highest + 1;
        db.store().branches.put(id, new common.DataTransfer.Branch(id, name));
        return id;
    }

    private static String money(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
