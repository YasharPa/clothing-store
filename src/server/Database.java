package server;

import common.Customer;
import common.CustomerType;
import common.DataTransfer.Branch;
import common.DataTransfer.Product;
import common.DataTransfer.SalesRecord;
import common.PasswordPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File based storage.
 * settings, branches, products, stock, customers, sales in the data folder after every change.
 * One record per line, fields separated by '|'.
 * All access must be done inside synchronized (db) { ... }, and every change must end with db.save().
 */
public class Database {

    // ================================================================ data

    /** Everything that is persisted. */
    public static class Store {
        public final Map<Integer, Branch> branches = new LinkedHashMap<>();
        public final Map<Integer, Product> products = new LinkedHashMap<>();
        public final Map<Integer, Map<Integer, Integer>> stock = new HashMap<>();   // branch -> (product -> qty)
        public final Map<String, Customer> customers = new LinkedHashMap<>();       // national id -> customer
        public final List<SalesRecord> sales = new ArrayList<>();
        public PasswordPolicy policy = PasswordPolicy.defaults();
        public int nextProductId = 1;
    }

    private static final String SETTINGS_FILE = "settings.txt";
    private static final String BRANCHES_FILE = "branches.txt";
    private static final String PRODUCTS_FILE = "products.txt";
    private static final String STOCK_FILE = "stock.txt";
    private static final String CUSTOMERS_FILE = "customers.txt";
    private static final String SALES_FILE = "sales.txt";

    private static final String[] ALL_FILES = {SETTINGS_FILE, BRANCHES_FILE, PRODUCTS_FILE, STOCK_FILE,
            CUSTOMERS_FILE, SALES_FILE};

    private final Path dir;
    private Store store;

    // ================================================================ creating the database

    /** Opens the database in the given folder, or creates it with sample data if there is none yet. */
    private static Database instance;

    /** The one Database that all client handlers share (created on first use, in the folder "data"). */
    public static synchronized Database getInstance() {
        if (instance == null) {
            try {
                instance = new Database(Path.of("data"));
            } catch (IOException e) {
                throw new IllegalStateException("Could not open the data folder: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public Database(Path dir) throws IOException {
        Files.createDirectories(dir);
        this.dir = dir;
        load();
    }

    /**
     * A throw-away database in a temp folder with a small fixed data set (2 branches, 3 products, 3 customers),
     * for tests and the demo, so every run starts from the same data. The real data is created by Database(Path).
     */
    public Database() {
        this.dir = createTempDir();
        this.store = new Store();
        seedSmall(store);
        save();
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("slice-db");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create a temp folder: " + e.getMessage(), e);
        }
    }

    // ================================================================ public API

    /** Direct access to the data. Callers must hold the lock: synchronized (db). */
    public Store store() {
        return store;
    }

    /** Writes the state to the text files. Call it after every change, while holding the lock. */
    public void save() {
        try {
            writeFile(SETTINGS_FILE, settingsLines());
            writeFile(BRANCHES_FILE, branchLines());
            writeFile(PRODUCTS_FILE, productLines());
            writeFile(STOCK_FILE, stockLines());
            writeFile(CUSTOMERS_FILE, customerLines());
            writeFile(SALES_FILE, salesLines());
        } catch (IOException e) {
            throw new IllegalStateException("Saving the data to the files failed: " + e.getMessage(), e);
        }
    }

    /** Helper for tests and the demo: adds one sale line and saves. */
    public void addSale(int branchId, int productId, int qty, double amount, LocalDateTime when) {
        Product p = store.products.get(productId);
        store.sales.add(new SalesRecord(branchId, productId, p.name(), p.category(), qty, amount, when,
                "demo", null));
        save();
    }

    // ================================================================ saving: one method per file

    private List<String> settingsLines() {
        PasswordPolicy p = store.policy;
        List<String> lines = new ArrayList<>();
        lines.add(line("policy", p.minLength(), p.requireDigit(), p.requireUpper(), p.requireSpecial()));
        lines.add(line("nextProductId", store.nextProductId));
        return lines;
    }

    private List<String> branchLines() {
        List<String> lines = new ArrayList<>();
        for (Branch b : store.branches.values()) {
            lines.add(line(b.id(), b.name()));
        }
        return lines;
    }

    private List<String> productLines() {
        List<String> lines = new ArrayList<>();
        for (Product p : store.products.values()) {
            lines.add(line(p.id(), p.name(), p.category(), p.price()));
        }
        return lines;
    }

    private List<String> stockLines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> branch : store.stock.entrySet()) {
            for (Map.Entry<Integer, Integer> item : branch.getValue().entrySet()) {
                lines.add(line(branch.getKey(), item.getKey(), item.getValue()));
            }
        }
        return lines;
    }

    private List<String> customerLines() {
        List<String> lines = new ArrayList<>();
        for (Customer c : store.customers.values()) {
            lines.add(line(c.getType().name(), c.getId(), c.getFullName(), c.getPhone(),
                    c.getPurchaseCount(), c.getTotalSpent()));
        }
        return lines;
    }

    private List<String> salesLines() {
        List<String> lines = new ArrayList<>();
        for (SalesRecord s : store.sales) {
            lines.add(line(s.branchId(), s.productId(), s.productName(), s.category(), s.qty(),
                    s.amount(), s.dateTime(), s.soldBy(), s.customerId()));
        }
        return lines;
    }

    /** Writes one file through a temp file, so a crash never leaves a half written file. */
    private void writeFile(String name, List<String> lines) throws IOException {
        Path tmp = dir.resolve(name + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    // ================================================================ loading

    /** Loads the files; if there are none (or they are corrupted) starts with the sample data. */
    private void load() throws IOException {
        if (Files.exists(dir.resolve(BRANCHES_FILE))) {
            try {
                store = readAll();
                System.out.println("[DB] Data loaded from the files in " + dir.toAbsolutePath());
                return;
            } catch (RuntimeException e) {
                moveFilesToBackup(e);
            }
        }
        store = new Store();
        seed(store);
        save();
        System.out.println("[DB] A new database with sample data was created: " + dir.toAbsolutePath());
    }

    /** Moves the broken files to a corrupt-&lt;time&gt; folder, so nothing is lost. */
    private void moveFilesToBackup(RuntimeException cause) throws IOException {
        Path backup = dir.resolve("corrupt-" + System.currentTimeMillis());
        Files.createDirectories(backup);
        for (String name : ALL_FILES) {
            if (Files.exists(dir.resolve(name))) {
                Files.move(dir.resolve(name), backup.resolve(name));
            }
        }
        System.err.println("[DB] The data files are corrupted (" + cause + "), moved to " + backup
                + ". A new database with sample data was created.");
    }

    private Store readAll() throws IOException {
        Store s = new Store();
        readSettings(s);
        readBranches(s);
        readProducts(s);
        readStock(s);
        readCustomers(s);
        readSales(s);
        return s;
    }

    private void readSettings(Store s) throws IOException {
        for (String[] f : readFile(SETTINGS_FILE)) {
            if (f[0].equals("policy")) {
                s.policy = new PasswordPolicy(Integer.parseInt(f[1]), Boolean.parseBoolean(f[2]),
                        Boolean.parseBoolean(f[3]), Boolean.parseBoolean(f[4]));
            } else if (f[0].equals("nextProductId")) {
                s.nextProductId = Integer.parseInt(f[1]);
            }
        }
    }

    private void readBranches(Store s) throws IOException {
        for (String[] f : readFile(BRANCHES_FILE)) {
            int id = Integer.parseInt(f[0]);
            s.branches.put(id, new Branch(id, f[1]));
        }
    }

    private void readProducts(Store s) throws IOException {
        for (String[] f : readFile(PRODUCTS_FILE)) {
            int id = Integer.parseInt(f[0]);
            s.products.put(id, new Product(id, f[1], f[2], Double.parseDouble(f[3])));
        }
    }

    private void readStock(Store s) throws IOException {
        for (String[] f : readFile(STOCK_FILE)) {
            int branchId = Integer.parseInt(f[0]);
            int productId = Integer.parseInt(f[1]);
            int qty = Integer.parseInt(f[2]);
            s.stock.computeIfAbsent(branchId, k -> new HashMap<>()).put(productId, qty);
        }
    }

    private void readCustomers(Store s) throws IOException {
        for (String[] f : readFile(CUSTOMERS_FILE)) {
            Customer c = Customer.create(CustomerType.valueOf(f[0]), f[1], f[2], f[3],
                    Integer.parseInt(f[4]), Double.parseDouble(f[5]));
            s.customers.put(c.getId(), c);
        }
    }

    private void readSales(Store s) throws IOException {
        for (String[] f : readFile(SALES_FILE)) {
            s.sales.add(new SalesRecord(Integer.parseInt(f[0]), Integer.parseInt(f[1]), f[2], f[3],
                    Integer.parseInt(f[4]), Double.parseDouble(f[5]), LocalDateTime.parse(f[6]), f[7], f[8]));
        }
    }

    /** Reads a file and splits every line into its fields. A missing file is an empty list. */
    private List<String[]> readFile(String name) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Path path = dir.resolve(name);
        if (!Files.exists(path)) {
            return rows;
        }
        for (String text : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!text.isEmpty()) {
                rows.add(splitLine(text));
            }
        }
        return rows;
    }

    // ================================================================ line format
    // One record per line, fields separated by '|'. Inside a field: \\ = backslash, \p = '|', \n = new line,
    // \r = carriage return, \N = null.

    private static String line(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    private static String escape(Object value) {
        if (value == null) {
            return "\\N";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toString().toCharArray()) {
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '|') {
                sb.append("\\p");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String[] splitLine(String text) {
        String[] parts = text.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = unescape(parts[i]);
        }
        return parts;
    }

    private static String unescape(String s) {
        if (s.equals("\\N")) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            i++;
            char next = s.charAt(i);
            if (next == 'p') {
                sb.append('|');
            } else if (next == 'n') {
                sb.append('\n');
            } else if (next == 'r') {
                sb.append('\r');
            } else {
                sb.append(next);
            }
        }
        return sb.toString();
    }

    private static void seed(Store s) {
        seedBranches(s);
        seedProducts(s);
        seedStock(s);
        seedCustomers(s);
    }

    private static void seedBranches(Store s) {
        s.branches.put(1, new Branch(1, "Tel Aviv"));
        s.branches.put(2, new Branch(2, "Haifa"));
        s.branches.put(3, new Branch(3, "Jerusalem"));
    }

    private static void seedProducts(Store s) {
        Object[][] products = {
                {"Cotton T-Shirt", "Tops", 89.90}, {"Polo Shirt", "Tops", 119.00},
                {"Tank Top", "Tops", 49.90}, {"Slim Jeans", "Pants", 199.00},
                {"Chino Pants", "Pants", 169.00}, {"Sweatpants", "Pants", 129.00},
                {"Summer Dress", "Dresses", 249.00}, {"Evening Dress", "Dresses", 399.00},
                {"Sneakers", "Shoes", 349.00}, {"Sandals", "Shoes", 149.00},
                {"Leather Belt", "Accessories", 79.00}, {"Scarf", "Accessories", 59.00}};
        for (Object[] p : products) {
            int id = s.nextProductId++;
            s.products.put(id, new Product(id, (String) p[0], (String) p[1], (Double) p[2]));
        }
    }

    private static void seedStock(Store s) {
        for (int branchId = 1; branchId <= 3; branchId++) {
            Map<Integer, Integer> quantities = new HashMap<>();
            for (int productId : s.products.keySet()) {
                quantities.put(productId, 5 + (branchId * 7 + productId * 3) % 20);
            }
            s.stock.put(branchId, quantities);
        }
    }

    /** The small data set used by the no-argument constructor. */
    private static void seedSmall(Store s) {
        s.branches.put(1, new Branch(1, "Tel Aviv"));
        s.branches.put(2, new Branch(2, "Haifa"));
        s.products.put(1, new Product(1, "Cotton T-Shirt", "Tops", 89.90));
        s.products.put(2, new Product(2, "Slim Jeans", "Pants", 199.00));
        s.products.put(3, new Product(3, "Sneakers", "Shoes", 349.00));
        s.nextProductId = 4;
        for (int branchId = 1; branchId <= 2; branchId++) {
            Map<Integer, Integer> quantities = new HashMap<>();
            for (int productId : s.products.keySet()) {
                quantities.put(productId, 10);
            }
            s.stock.put(branchId, quantities);
        }
        seedCustomers(s);
    }

    private static void seedCustomers(Store s) {
        s.customers.put("200000001", Customer.create(CustomerType.NEW, "200000001", "Shira Alon", "0521111111"));
        s.customers.put("200000002", Customer.create(CustomerType.RETURNING, "200000002", "Gil Barak", "0522222222", 3, 650));
        s.customers.put("200000003", Customer.create(CustomerType.VIP, "200000003", "Liat Nachum", "0523333333", 12, 5400));
    }
}