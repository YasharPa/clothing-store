package server;

import common.Role;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run this once to create data/users.txt with a few TEST accounts.
 * These passwords are for development only; real accounts are made from the admin screen.
 */
public class SeedUsers {

    public static final Path USERS_FILE = Path.of("data", "users.txt");

    public static void main(String[] args) throws IOException {
        if (Files.exists(USERS_FILE)) {
            System.out.println(USERS_FILE + " already exists. Delete it first if you want to recreate it.");
            return;
        }

        UserStore store = new UserStore(USERS_FILE);
        store.addUser("admin", "admin-test-1", Role.ADMIN, "HQ");
        store.addUser("manager", "manager-test-1", Role.SHIFT_MANAGER, "Tel Aviv");
        store.addUser("cashier", "cashier-test-1", Role.CASHIER, "Tel Aviv");
        store.addUser("seller", "seller-test-1", Role.SALESPERSON, "Haifa");

        System.out.println("Created " + USERS_FILE + " with 4 test accounts (usernames: admin, manager, cashier, seller).");
    }
}
