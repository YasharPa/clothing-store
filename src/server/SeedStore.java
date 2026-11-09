package server;

import common.Category;
import common.Product;
import server.inventory.BranchInventory;
import server.inventory.FileInventoryRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run this once (like SeedUsers) to create a TEST catalog of products and a TEST stock for three branches.
 * Real products and stock are added by the inventory screens. Does nothing if the files already exist.
 */
public class SeedStore {

    private static final String[] BRANCHES = {"Tel Aviv", "Haifa", "Jerusalem"};

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(System.getProperty("data.dir", "data"));
        Path productsFile = dir.resolve("inventory-products.txt");
        Path stockFile = dir.resolve("inventory-stock.txt");
        if (Files.exists(productsFile)) {
            System.out.println(productsFile + " already exists. Delete it first if you want to recreate it.");
            return;
        }
        Files.createDirectories(dir);

        FileInventoryRepository repository = new FileInventoryRepository(productsFile, stockFile);
        Object[][] products = {
                {"Cotton T-Shirt", Category.SHIRTS, 89.90}, {"Polo Shirt", Category.SHIRTS, 119.00},
                {"Tank Top", Category.SHIRTS, 49.90}, {"Slim Jeans", Category.PANTS, 199.00},
                {"Chino Pants", Category.PANTS, 169.00}, {"Sweatpants", Category.PANTS, 129.00},
                {"Summer Dress", Category.DRESSES, 249.00}, {"Evening Dress", Category.DRESSES, 399.00},
                {"Sneakers", Category.SHOES, 349.00}, {"Sandals", Category.SHOES, 149.00},
                {"Leather Belt", Category.ACCESSORIES, 79.00}, {"Scarf", Category.ACCESSORIES, 59.00}};
        int id = 1;
        for (Object[] p : products) {
            repository.addProduct(new Product(id, (String) p[0], (Category) p[1], (Double) p[2]));
            id++;
        }
        for (String branch : BRANCHES) {
            BranchInventory inventory = repository.findByBranch(branch);
            for (int productId = 1; productId < id; productId++) {
                inventory.addStock(productId, 5 + (branch.length() * 7 + productId * 3) % 20);
            }
            repository.save(inventory);
        }
        System.out.println("Created the test products and stock for " + BRANCHES.length + " branches in " + dir);
    }
}
