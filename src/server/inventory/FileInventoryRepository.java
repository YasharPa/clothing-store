package server.inventory;

import common.Category;
import common.Product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A repository kept in two text files on the server, so the stock survives a restart.
 * <ul>
 *   <li>products file, one product per line: {@code id;name;CATEGORY;price}</li>
 *   <li>inventory file, one line per branch and product: {@code branch;productId;quantity}</li>
 * </ul>
 * The data lives in memory (in an InMemoryInventoryRepository); this class reads the files when it starts
 * and rewrites them after every change. It USES an in-memory repository instead of extending it
 * (composition): it only adds "and also write it to a file".
 */
public class FileInventoryRepository implements InventoryRepository {

    private static final String SEPARATOR = ";";

    private final Path productsFile;
    private final Path inventoryFile;
    private final InMemoryInventoryRepository memory = new InMemoryInventoryRepository();
    // The last saved state of every branch: branch -> (product id -> quantity). Written to the inventory file.
    private final Map<String, Map<Integer, Integer>> savedBranches = new TreeMap<>();

    /**
     * Opens the repository and reads both files. A file that does not exist yet means "empty".
     *
     * @throws IOException if a file exists but cannot be read or has a bad line
     */
    public FileInventoryRepository(Path productsFile, Path inventoryFile) throws IOException {
        this.productsFile = productsFile;
        this.inventoryFile = inventoryFile;
        if (Files.exists(productsFile)) {
            loadProducts();
        }
        if (Files.exists(inventoryFile)) {
            loadInventory();
        }
    }

    /**
     * Adds a product to the catalog (or replaces the one with the same id) and saves the products file.
     *
     * @throws IllegalArgumentException if the name contains ';' or a line break
     * @throws IOException              if the file could not be written; the product is not added then
     */
    public synchronized void addProduct(Product product) throws IOException {
        checkText("product name", product.getName());
        Optional<Product> previous = memory.findProduct(product.getId());
        memory.addProduct(product);
        try {
            writeProducts();
        } catch (IOException e) {
            // Do not keep in memory what is not in the file: it would vanish at the next restart.
            previous.ifPresentOrElse(memory::addProduct, () -> memory.removeProduct(product.getId()));
            throw e;
        }
    }

    @Override
    public List<Product> findAllProducts() {
        return memory.findAllProducts();
    }

    @Override
    public Optional<Product> findProduct(int productId) {
        return memory.findProduct(productId);
    }

    @Override
    public BranchInventory findByBranch(String branch) {
        return memory.findByBranch(branch);
    }

    @Override
    public List<String> findAllBranches() {
        return memory.findAllBranches();
    }

    /**
     * Rewrites the inventory file with the branch's current quantities.
     *
     * The branch is read BEFORE taking this repository's lock. So a thread never holds the repository's lock
     * while waiting for a branch's lock, and two threads can never wait for each other forever (deadlock).
     */
    @Override
    public void save(BranchInventory inventory) throws IOException {
        checkText("branch name", inventory.getBranch());
        Map<Integer, Integer> quantities = inventory.getAll();
        synchronized (this) {
            Map<Integer, Integer> previous = savedBranches.put(inventory.getBranch(), quantities);
            try {
                writeInventory();
            } catch (IOException e) {
                if (previous == null) {
                    savedBranches.remove(inventory.getBranch());
                } else {
                    savedBranches.put(inventory.getBranch(), previous);
                }
                throw e;
            }
        }
    }

    private void loadProducts() throws IOException {
        List<String> lines = Files.readAllLines(productsFile, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            String[] parts = lines.get(i).split(SEPARATOR, -1);
            if (parts.length != 4) {
                throw badLine(productsFile, i, null);
            }
            try {
                memory.addProduct(new Product(Integer.parseInt(parts[0]), parts[1],
                        Category.valueOf(parts[2]), Double.parseDouble(parts[3])));
            } catch (IllegalArgumentException e) { // also catches NumberFormatException
                throw badLine(productsFile, i, e);
            }
        }
    }

    private void loadInventory() throws IOException {
        List<String> lines = Files.readAllLines(inventoryFile, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            String[] parts = lines.get(i).split(SEPARATOR, -1);
            if (parts.length != 3) {
                throw badLine(inventoryFile, i, null);
            }
            try {
                String branch = parts[0];
                int productId = Integer.parseInt(parts[1]);
                int quantity = Integer.parseInt(parts[2]);
                if (quantity < 0) {
                    throw badLine(inventoryFile, i, null);
                }
                BranchInventory inventory = memory.findByBranch(branch);
                if (quantity > 0) {
                    inventory.addStock(productId, quantity);
                }
                savedBranches.put(branch, inventory.getAll());
            } catch (IllegalArgumentException e) {
                throw badLine(inventoryFile, i, e);
            }
        }
    }

    private void writeProducts() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Product product : memory.findAllProducts()) {
            lines.add(String.join(SEPARATOR, String.valueOf(product.getId()), product.getName(),
                    product.getCategory().name(), String.valueOf(product.getPrice())));
        }
        write(productsFile, lines);
    }

    private void writeInventory() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Integer>> branch : savedBranches.entrySet()) {
            // TreeMap: the products of a branch are written sorted by id, so the file is easy to read.
            for (Map.Entry<Integer, Integer> stock : new TreeMap<>(branch.getValue()).entrySet()) {
                lines.add(branch.getKey() + SEPARATOR + stock.getKey() + SEPARATOR + stock.getValue());
            }
        }
        write(inventoryFile, lines);
    }

    private static void write(Path file, List<String> lines) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void checkText(String name, String value) {
        if (value.contains(SEPARATOR) || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException("The " + name + " must not contain ';' or a line break");
        }
    }

    private static IOException badLine(Path file, int index, Exception cause) {
        return new IOException("Bad line " + (index + 1) + " in " + file, cause);
    }
}
