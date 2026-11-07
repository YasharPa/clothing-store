package server;

import common.Employee;
import common.Role;

/** Ready-made employees for the tests. */
final class TestEmployees {
    private TestEmployees() {
    }

    static Employee employee(String username, Role role) {
        return new Employee(username, "Test " + username, "300000001", "0501111111", "12345001", 1, 1001, role);
    }
}
