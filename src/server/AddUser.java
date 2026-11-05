package server;

import common.Role;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A small development tool: run it to add one user to data/users.txt.
 * Stop and restart the server afterwards, because the server reads the file only when it starts.
 * (Real accounts will be created from the admin screen.)
 */
public class AddUser {

    public static void main(String[] args) throws IOException {
        Scanner keyboard = new Scanner(System.in);
        UserStore store = new UserStore(SeedUsers.USERS_FILE);

        System.out.print("Username: ");
        String username = keyboard.nextLine().trim();
        System.out.print("Password: ");
        String password = keyboard.nextLine();
        System.out.print("Role " + Arrays.toString(Role.values()) + ": ");
        String roleText = keyboard.nextLine().trim().toUpperCase();
        System.out.print("Branch: ");
        String branch = keyboard.nextLine().trim();

        Role role;
        try {
            role = Role.valueOf(roleText);
        } catch (IllegalArgumentException e) {
            System.out.println("Could not add the user: unknown role \"" + roleText + "\"");
            return;
        }

        try {
            store.addUser(username, password, role, branch);
            System.out.println("Added \"" + username + "\". Restart the server so it sees the new user.");
        } catch (IllegalArgumentException e) {
            // Everything UserStore.addUser refuses: empty fields, a ';', a name that is taken.
            System.out.println("Could not add the user: " + e.getMessage());
        }
    }
}
