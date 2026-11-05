package server;

import common.Role;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A small development tool: run it to add one user to data/users.txt.
 *
 * Use it ONLY while the server is stopped, for example to create the very first admin.
 * While the server is running it owns the file and keeps its own copy in memory, so a user
 * added here would be missed, and could even be wiped by the server's next save.
 * With the server running, an admin creates users with the ADD_USER request instead.
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
            System.out.println("Added \"" + username + "\". The server must NOT be running while you use this tool.");
        } catch (IllegalArgumentException e) {
            // Everything UserStore.addUser refuses: empty fields, a ';', a name that is taken.
            System.out.println("Could not add the user: " + e.getMessage());
        }
    }
}
