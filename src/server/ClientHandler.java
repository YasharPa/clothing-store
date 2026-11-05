package server;

import common.Credentials;
import common.NewAccount;
import common.Request;
import common.Response;
import common.Role;
import common.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;

/**
 * The worker that talks to ONE client. The server creates one ClientHandler
 * (running in its own thread) for every client that connects.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final UserStore userStore;
    private final LoggedInUsers loggedInUsers;

    /** Who is logged in on this connection. Null until a login succeeds. */
    private User user;

    public ClientHandler(Socket socket, UserStore userStore, LoggedInUsers loggedInUsers) {
        this.socket = socket;
        this.userStore = userStore;
        this.loggedInUsers = loggedInUsers;
    }

    /** The job of the thread: talk to the client until it disconnects. */
    @Override
    public void run() {
        String client = String.valueOf(socket.getRemoteSocketAddress());
        // try (socket): closes the socket automatically when this client is done.
        try (socket) {
            System.out.println("Client connected: " + client);
            handleClient();
            System.out.println("Client disconnected: " + client);
        } catch (SocketException e) {
            // The connection broke (client crashed, window killed, network dropped).
            System.out.println("Lost connection to " + client + ": " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            // A problem with ONE client must not stop the server or the other clients.
            System.err.println("Problem with client " + client + ": " + e);
        } finally {
            // Runs however the connection ended (goodbye, crash, error), so the user is never stuck "logged in".
            logOut();
        }
    }

    /**
     * Removes this connection's user from the list of logged-in users. Safe to call when nobody is logged in.
     * Not private so that the tests can pretend the connection ended.
     */
    void logOut() {
        if (user != null) {
            loggedInUsers.logOut(user.getUsername());
            System.out.println("Logged out: " + user);
            user = null;
        }
    }

    /** Reads Requests from the client and sends back a Response for each, until the client disconnects. */
    private void handleClient() throws IOException, ClassNotFoundException {
        // The output stream must be created BEFORE the input stream (on both sides),
        // otherwise the two programs wait for each other forever.
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            Object message;
            try {
                message = in.readObject();
            } catch (EOFException e) {
                return; // the client hung up normally
            }

            Response response;
            if (message instanceof Request request) {
                // Only the type is logged: a LOGIN request contains a password.
                System.out.println("Received from " + socket.getRemoteSocketAddress() + ": " + request.getType());
                response = process(request);
            } else {
                // Something that is not a Request arrived: answer with an error, don't crash.
                response = new Response(false, "Unknown message");
            }
            out.writeObject(response);
            out.flush();
        }
    }

    /**
     * Decides what to do with a request, based on its type.
     * Not private so that the tests in the same package can call it.
     */
    Response process(Request request) {
        if (request.getType() == null) {
            return new Response(false, "The request has no type");
        }
        return switch (request.getType()) {
            case LOGIN -> login(request);
            case LOGOUT -> handleLogout();
            case ADD_USER -> addUser(request);
            case ECHO -> echo(request);
        };
    }

    private Response login(Request request) {
        if (user != null) {
            return new Response(false, "Already logged in");
        }
        if (!(request.getPayload() instanceof Credentials credentials)) {
            return new Response(false, "A login needs a username and a password");
        }

        Optional<User> result = userStore.authenticate(credentials.getUsername(), credentials.getPassword());
        if (result.isEmpty()) {
            System.out.println("Failed login for \"" + credentials.getUsername() + "\"");
            // Same message for a wrong username and a wrong password, so nobody can find out which users exist.
            return new Response(false, "Wrong username or password");
        }

        // The password is right. Only now do we check whether this user is already on another computer
        // (checking first would tell anyone which users are online, without knowing a password).
        User found = result.get();
        if (!loggedInUsers.logIn(found.getUsername())) {
            System.out.println("Refused a second login for \"" + found.getUsername() + "\"");
            return new Response(false, "This user is already logged in on another computer");
        }

        user = found;
        System.out.println("Logged in: " + user);
        return new Response(true, "Welcome, " + user.getUsername(), user);
    }

    /** The client asked to log out (for example the "Log out" button). The connection itself stays open. */
    private Response handleLogout() {
        if (user == null) {
            return new Response(false, "Not logged in");
        }
        logOut();
        return new Response(true, "Logged out");
    }

    /** Creates a new user. Authorization: only a logged-in ADMIN is allowed. */
    private Response addUser(Request request) {
        if (user == null) {
            return new Response(false, "Please log in first");
        }
        if (user.getRole() != Role.ADMIN) {
            System.out.println(user.getUsername() + " tried to add a user but is not an admin");
            return new Response(false, "Not allowed: only an admin can add users");
        }
        if (!(request.getPayload() instanceof NewAccount account)) {
            return new Response(false, "Adding a user needs the account details");
        }

        try {
            userStore.addUser(account.getUsername(), account.getPassword(), account.getRole(), account.getBranch());
        } catch (IllegalArgumentException e) {
            return new Response(false, e.getMessage()); // for example: the username already exists
        } catch (IOException e) {
            System.err.println("Could not save the new user: " + e);
            return new Response(false, "The server could not save the new user");
        }

        System.out.println(user.getUsername() + " added the user \"" + account.getUsername() + "\"");
        return new Response(true, "Added the user " + account.getUsername());
    }

    private Response echo(Request request) {
        if (user == null) {
            return new Response(false, "Please log in first");
        }
        if (!(request.getPayload() instanceof String text)) {
            return new Response(false, "An echo needs a text");
        }
        return new Response(true, "Echo: " + text);
    }
}
