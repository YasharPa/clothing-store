package common;

import java.io.Serializable;

/**
 * What the client sends to log in: a username and a password.
 * The password travels to the server only here, and the server must never log or store it.
 */
public class Credentials implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
