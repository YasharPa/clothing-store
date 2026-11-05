package common;

import java.io.Serializable;

/**
 * The details of an account an admin wants to create.
 * The password travels to the server only here, and the server must never log or store it as it is.
 */
public class NewAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    private final Role role;
    private final String branch;

    public NewAccount(String username, String password, Role role, String branch) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.branch = branch;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public String getBranch() {
        return branch;
    }
}
