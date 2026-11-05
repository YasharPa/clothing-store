package server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The names of the users who are logged in right now, shared by all the ClientHandlers.
 * It is how the server stops the same user from being logged in on two computers.
 */
public class LoggedInUsers {

    // A thread-safe set: many handlers (threads) use it at the same time.
    private final Set<String> usernames = ConcurrentHashMap.newKeySet();

    /**
     * Marks the user as logged in.
     *
     * @return true if it worked, false if this user was already logged in
     */
    public boolean logIn(String username) {
        // add() checks and adds in ONE step, so two simultaneous logins cannot both succeed.
        return usernames.add(username);
    }

    /** Marks the user as logged out. Does nothing if they were not logged in. */
    public void logOut(String username) {
        usernames.remove(username);
    }

    /** True if this user is logged in on some computer right now. */
    public boolean isLoggedIn(String username) {
        return usernames.contains(username);
    }
}
