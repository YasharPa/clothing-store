package demo;

import client.LogsMenu;
import client.ReportExporter;
import common.Customer;
import common.CustomerType;
import common.DataTransfer.LogEntry;
import common.DataTransfer.ReportMode;
import common.DataTransfer.ReportRequest;
import common.DataTransfer.ReportResult;
import common.Employee;
import common.LogType;
import common.Role;
import common.ServiceException;
import server.Database;
import server.LogQueryService;
import server.LogService;
import server.ReportService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A console demo of part 3 (customers, reports, logs) that runs by itself, without a server.
 * It uses the small test database in a temporary folder, so nothing in your real data folder changes.
 * You are logged in as a system admin, so you may read the log.
 */
public class Part3Demo {
    private static final Employee ADMIN = new Employee("admin", "System Admin", "300000001", "0501111111",
            "12345001", 0, 1001, Role.ADMIN);

    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private final Database db = new Database();
    private final ReportService reports = new ReportService(db);
    private final LogQueryService logQuery = new LogQueryService();
    private final LogService log = LogService.getInstance();

    public static void main(String[] args) throws IOException {
        new Part3Demo().run();
    }

    private void run() throws IOException {
        addSampleSales();
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== Part 3 demo ===");
            System.out.println("1. Customers: show the three types");
            System.out.println("2. Customers: make a purchase");
            System.out.println("3. Reports");
            System.out.println("4. System log");
            System.out.println("0. Exit");
            String choice = ask("Choice: ");
            try {
                if (choice == null || choice.equals("0")) {
                    running = false;
                } else if (choice.equals("1")) {
                    showCustomers();
                } else if (choice.equals("2")) {
                    makePurchase();
                } else if (choice.equals("3")) {
                    makeReport();
                } else if (choice.equals("4")) {
                    showLog();
                } else {
                    System.out.println("Please choose 0-4");
                }
            } catch (ServiceException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private String ask(String question) throws IOException {
        System.out.print(question);
        return in.readLine();
    }

    private void addSampleSales() {
        db.addSale(1, 1, 2, 179.80, LocalDateTime.of(2026, 1, 5, 10, 0));
        db.addSale(1, 2, 1, 199.00, LocalDateTime.of(2026, 1, 6, 11, 0));
        db.addSale(2, 1, 3, 269.70, LocalDateTime.of(2026, 1, 5, 12, 0));
        db.addSale(2, 3, 1, 349.00, LocalDateTime.of(2026, 1, 6, 15, 0));
    }

    // ---- customers ----

    private void showCustomers() {
        for (Customer c : db.store().customers.values()) {
            System.out.println();
            System.out.println(c + " - purchases: " + c.getPurchaseCount() + ", spent: " + c.getTotalSpent());
            System.out.println("  " + c.getPurchaseTrack());
            System.out.println("  Promotion: " + c.getPromotionDescription());
        }
    }

    private void makePurchase() throws IOException {
        List<Customer> list = new ArrayList<>(db.store().customers.values());
        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ". " + list.get(i));
        }
        int number = Integer.parseInt(ask("Which customer? ").trim());
        Customer customer = list.get(number - 1);
        double total = Double.parseDouble(ask("Total of the purchase: ").trim());

        double paid = customer.applyPromotion(total);
        String gift = customer.getGift(paid);
        customer.recordPurchase(paid);
        System.out.println("Promotion: " + customer.getPromotionDescription());
        System.out.println("Total " + total + " -> to pay " + paid);
        if (gift != null) {
            System.out.println("Gift: " + gift);
        }

        CustomerType next = customer.jobTransition();
        if (next != customer.getType()) {
            Customer upgraded = Customer.convert(customer, next);
            db.store().customers.put(upgraded.getId(), upgraded);
            System.out.println("The customer is now: " + next.getLabel());
        }
        db.save();
    }

    // ---- reports ----

    private void makeReport() throws IOException {
        System.out.println("1. By branch   2. By product   3. By category");
        String type = ask("Report type: ").trim();
        ReportMode mode = ReportMode.BY_BRANCH;
        if (type.equals("2")) {
            mode = ReportMode.BY_PRODUCT;
        } else if (type.equals("3")) {
            mode = ReportMode.BY_CATEGORY;
        }
        int branchId = Integer.parseInt(ask("Branch number (0 = whole network): ").trim());
        String dateText = ask("Date yyyy-MM-dd (empty = whole period): ").trim();
        LocalDate date = null;
        if (!dateText.isEmpty()) {
            date = LocalDate.parse(dateText);
        }
        ReportRequest request = new ReportRequest(mode, branchId, date, new ArrayList<>(), new ArrayList<>());

        ReportResult report = reports.generate(ADMIN, request);

        System.out.println();
        System.out.println(report.asText());
        Path file = ReportExporter.save(report);
        System.out.println("Saved to " + file);
        ReportExporter.openInNotepad(file);
    }

    // ---- log ----

    private void showLog() throws IOException {
        log.log(LogType.LOGIN, ADMIN.getUsername(), "Opened the log screen in the demo");
        new LogsMenu(in, System.out, query -> logQuery.read(ADMIN, query)).run();
    }
}