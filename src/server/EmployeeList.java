package server;

import common.Role;
import common.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the list of all the employees (the users) from the users file that UserStore writes.
 * Only the username, role and branch are read: the salt and the password hash never leave this class.
 * It only reads, so UserStore stays as it is.
 */
public class EmployeeList {
    private final Path file;

    /** @param file the users file, the same one that UserStore uses (data/users.txt) */
    public EmployeeList(Path file) {
        this.file = file;
    }

    /** All the employees in the order of the file. An empty list if there is no file yet. */
    public List<User> readAll() {
        List<User> employees = new ArrayList<>();
        if (!Files.exists(file)) {
            return employees;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split(";", -1);
                if (parts.length == 5) {
                    employees.add(new User(parts[0], Role.valueOf(parts[3]), parts[4]));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not read the employees: " + e.getMessage(), e);
        }
        return employees;
    }
}