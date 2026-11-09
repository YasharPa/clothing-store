package client;

import common.Credentials;
import common.Category;
import common.Customer;
import common.CustomerType;
import common.DataTransfer.CustomerForm;
import common.DataTransfer.LogEntry;
import common.DataTransfer.ReportMode;
import common.DataTransfer.ReportRequest;
import common.DataTransfer.ReportResult;
import common.DataTransfer.SaleForm;
import common.DataTransfer.StockChange;
import common.NewAccount;
import common.Protocol;
import common.Request;
import common.RequestType;
import common.Response;
import common.Role;
import common.ServiceException;
import common.StockItem;
import common.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A console menu for part 3 (customers, reports, logs) to try it against the real server.
 * Run the server first. Requests are sent the same way as in LoginWindow: connection.send(new Request(...)).
 */
public class Console {

    /** Whether the logged-in user may see every branch (a shift manager and an admin). Others see only their own. */
    private static boolean seesAllBranches = false;

    /** The branch the logged-in user belongs to. */
    private static String myBranch = "";

    /** An admin belongs to no branch, so he chooses which branch's inventory to look at. */
    private static boolean isAdmin = false;

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try (ServerConnection connection = new ServerConnection(Protocol.HOST, Protocol.PORT)) {
            if (!logIn(connection, in)) {
                return;
            }
            boolean running = true;
            while (running) {
                System.out.println();
                System.out.println("1. Show customers");
                System.out.println("2. Register a customer");
                System.out.println("3. Reports");
                System.out.println("4. System log");
                System.out.println("5. Show the inventory of my branch");
                System.out.println("6. Sell a product");
                System.out.println("7. Buy stock");
                if (isAdmin) {
                    System.out.println("8. Manage employees");
                }
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                String choice = in.readLine();
                if (choice == null || choice.equals("0")) {
                    running = false;
                } else {
                    try {
                        runChoice(choice.trim(), connection, in);
                    } catch (ServiceException e) {
                        System.out.println("Error: " + e.getMessage());
                    } catch (IllegalArgumentException | DateTimeException e) {
                        System.out.println("Error: that input is not valid (" + e.getMessage() + ")");
                    }
                }
            }
        }
    }

    private static boolean logIn(ServerConnection connection, BufferedReader in) throws IOException {
        System.out.print("Username: ");
        String username = in.readLine();
        System.out.print("Password: ");
        String password = in.readLine();
        if (username == null || password == null) {
            return false;
        }
        Response response = connection.send(new Request(RequestType.LOGIN, new Credentials(username, password)));
        System.out.println(response.getMessage());
        if (response.isSuccess()) {
            User user = (User) response.getData();
            seesAllBranches = user.getRole() == Role.ADMIN || user.getRole() == Role.SHIFT_MANAGER;
            myBranch = user.getBranch();
            isAdmin = user.getRole() == Role.ADMIN;
        }
        return response.isSuccess();
    }

    @SuppressWarnings("unchecked")
    private static void runChoice(String choice, ServerConnection connection, BufferedReader in) throws IOException {
        if (choice.equals("1")) {
            List<Customer> customers = (List<Customer>) send(connection, RequestType.GET_CUSTOMERS, null).getData();
            for (Customer c : customers) {
                System.out.println(c + "  purchases: " + c.getPurchaseCount() + ", spent: " + c.getTotalSpent());
            }
        } else if (choice.equals("2")) {
            System.out.print("ID (9 digits): ");
            String id = in.readLine();
            System.out.print("Full name: ");
            String name = in.readLine();
            System.out.print("Phone: ");
            String phone = in.readLine();
            CustomerForm form = new CustomerForm(CustomerType.NEW, id, name, phone);
            System.out.println(send(connection, RequestType.ADD_CUSTOMER, form).getMessage());
        } else if (choice.equals("3")) {
            runReport(connection, in);
        } else if (choice.equals("4")) {
            new LogsMenu(in, System.out, query -> {
                try {
                    return (List<LogEntry>) send(connection, RequestType.GET_LOGS, query).getData();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).run();
        } else if (choice.equals("5")) {
            String branchName = null;
            if (isAdmin) {
                branchName = askBranchName(connection, in);
            }
            for (StockItem item : getInventory(connection, branchName)) {
                System.out.println(item.product() + "  in stock: " + item.quantity());
            }
        } else if (choice.equals("6")) {
            System.out.print("Product number: ");
            int productId = Integer.parseInt(in.readLine().trim());
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(in.readLine().trim());
            System.out.print("Customer ID (empty = no customer): ");
            String customerId = in.readLine().trim();
            if (customerId.isEmpty()) {
                customerId = null;
            }
            SaleForm sale = new SaleForm(productId, quantity, customerId);
            System.out.println(send(connection, RequestType.SELL_ITEM, sale).getMessage());
        } else if (choice.equals("7")) {
            System.out.print("Product number: ");
            int productId = Integer.parseInt(in.readLine().trim());
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(in.readLine().trim());
            StockChange change = new StockChange(productId, quantity);
            System.out.println(send(connection, RequestType.RESTOCK_ITEM, change).getMessage());
        } else if (choice.equals("8") && isAdmin) {
            manageEmployees(connection, in);
        } else {
            System.out.println("Please choose from the menu");
        }
    }
    /** Asks what kind of report, for which branch, which products or categories and which date; then shows it. */
    @SuppressWarnings("unchecked")
    private static void runReport(ServerConnection connection, BufferedReader in) throws IOException {
        System.out.println("1. By branch");
        System.out.println("2. By product");
        System.out.println("3. By category");
        System.out.print("Report type: ");
        String type = in.readLine().trim();
        ReportMode mode = ReportMode.BY_BRANCH;
        if (type.equals("2")) {
            mode = ReportMode.BY_PRODUCT;
        } else if (type.equals("3")) {
            mode = ReportMode.BY_CATEGORY;
        }

        List<String> branches = (List<String>) send(connection, RequestType.GET_BRANCHES, null).getData();
        int branchId = 0;
        if (seesAllBranches) {
            System.out.println("0 - the whole network");
            for (String branch : branches) {
                System.out.println(branch);
            }
            System.out.print("Branch number: ");
            branchId = Integer.parseInt(in.readLine().trim());
        } else {
            // A cashier and a salesperson see only the branch they belong to.
            for (String branch : branches) {
                if (branch.endsWith(" - " + myBranch)) {
                    System.out.println(branch);
                    branchId = Integer.parseInt(branch.substring(0, branch.indexOf(' ')));
                }
            }
            System.out.print("Branch number (" + myBranch + "): ");
            in.readLine();
        }

        List<Integer> productIds = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        if (mode == ReportMode.BY_PRODUCT) {
            for (StockItem item : getInventory(connection, null)) {
                System.out.println(item.product());
            }
            System.out.print("Product numbers, separated by commas (empty = all the products): ");
            for (String number : in.readLine().split(",")) {
                if (!number.isBlank()) {
                    productIds.add(Integer.parseInt(number.trim()));
                }
            }
        } else if (mode == ReportMode.BY_CATEGORY) {
            Set<Category> known = new LinkedHashSet<>();
            for (StockItem item : getInventory(connection, null)) {
                known.add(item.product().getCategory());
            }
            List<Category> list = new ArrayList<>(known);
            for (int i = 0; i < list.size(); i++) {
                System.out.println((i + 1) + ". " + list.get(i));
            }
            System.out.print("Category number: ");
            int number = Integer.parseInt(in.readLine().trim());
            if (number < 1 || number > list.size()) {
                throw new ServiceException("Please choose a category from the list");
            }
            categories.add(list.get(number - 1).name());
        }

        System.out.print("Date yyyy-MM-dd (empty = the whole period): ");
        String dateText = in.readLine().trim();
        LocalDate date = null;
        if (!dateText.isEmpty()) {
            date = LocalDate.parse(dateText);
        }

        ReportRequest request = new ReportRequest(mode, branchId, date, productIds, categories);
        ReportResult report = (ReportResult) send(connection, RequestType.GET_REPORT, request).getData();
        System.out.println();
        System.out.println(report.asText());

        System.out.print("Open the report in Notepad? (y/n): ");
        if (in.readLine().trim().equalsIgnoreCase("y")) {
            Path file = ReportExporter.save(report);
            System.out.println("Saved to " + file);
            ReportExporter.openInNotepad(file);
        }
    }
    /** The inventory of a branch (null = the logged-in user's own branch). */
    @SuppressWarnings("unchecked")
    private static List<StockItem> getInventory(ServerConnection connection, String branchName) throws IOException {
        return (List<StockItem>) send(connection, RequestType.GET_INVENTORY, branchName).getData();
    }

    /** Shows the branches with their numbers and returns the name of the one the user chooses. */
    @SuppressWarnings("unchecked")
    private static String askBranchName(ServerConnection connection, BufferedReader in) throws IOException {
        List<String> branches = (List<String>) send(connection, RequestType.GET_BRANCHES, null).getData();
        for (String branch : branches) {
            System.out.println(branch);
        }
        System.out.print("Branch number: ");
        String number = in.readLine().trim();
        for (String branch : branches) {
            if (branch.startsWith(number + " - ")) {
                return branch.substring(branch.indexOf(" - ") + 3);
            }
        }
        throw new ServiceException("Please choose a branch from the list");
    }

    /** Sends one request. A failed answer becomes a ServiceException with the server's message. */
    private static Response send(ServerConnection connection, RequestType type, Serializable payload)
            throws IOException {
        Response response = connection.send(new Request(type, payload));
        if (!response.isSuccess()) {
            throw new ServiceException(response.getMessage());
        }
        return response;
    }
    /** The employee screen of the admin. Adding works; the other actions are not supported yet. */
    private static void manageEmployees(ServerConnection connection, BufferedReader in) throws IOException {
        System.out.println("1. Add an employee");
        System.out.println("2. Update an employee");
        System.out.println("3. Delete an employee");
        System.out.println("4. Change a password");
        System.out.print("Choice: ");
        String choice = in.readLine().trim();
        if (choice.equals("1")) {
            System.out.print("Username: ");
            String username = in.readLine().trim();
            System.out.print("Password: ");
            String password = in.readLine().trim();
            System.out.print("Role " + Arrays.toString(Role.values()) + ": ");
            Role role = Role.valueOf(in.readLine().trim().toUpperCase());
            System.out.print("Branch: ");
            String branch = in.readLine().trim();
            NewAccount account = new NewAccount(username, password, role, branch);
            System.out.println(send(connection, RequestType.ADD_USER, account).getMessage());
        } else if (choice.equals("2") || choice.equals("3") || choice.equals("4")) {
            System.out.println("Not supported yet.");
        } else {
            System.out.println("Please choose 1-4");
        }
    }
}