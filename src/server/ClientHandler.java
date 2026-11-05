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

    /** Who is logged in on this connection. Null until a login succeeds. */
    private User user;

    public ClientHandler(Socket socket, UserStore userStore) {
        this.socket = socket;
        this.userStore = userStore;
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

        user = result.get();
        System.out.println("Logged in: " + user);
        return new Response(true, "Welcome, " + user.getUsername(), user);
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
