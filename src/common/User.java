package common;

import java.io.Serializable;

/**
 * Who is logged in: username, role and branch.
 * It is sent from the server to the client after a successful login,
 * so it must NEVER contain the password.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final Role role;
    private final String branch;

    public User(String username, Role role, String branch) {
        this.username = username;
        this.role = role;
        this.branch = branch;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getBranch() {
        return branch;
    }

    @Override
    public String toString() {
        return username + " (" + role + ", branch " + branch + ")";
    }
}
