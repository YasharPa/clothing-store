package client;

import common.Credentials;
import common.NewAccount;
import common.Protocol;
import common.Request;
import common.RequestType;
import common.Response;
import common.Role;
import common.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * A text client for testing: it connects to the server, asks you to log in, and then
 * sends what you type as ECHO requests. Special commands:
 *   /adduser  creates a new user (the server only allows it for an ADMIN)
 *   bye       quits
 * (The login window replaces the typed prompts in Step 10.)
 */
public class Client {

    public static void main(String[] args) {
        try (Socket socket = new Socket(Protocol.HOST, Protocol.PORT);
             // Output stream first, then input stream (see the note in ClientHandler.handleClient).
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner keyboard = new Scanner(System.in)) {

            System.out.println("Connected to the server.");

            User user = logIn(out, in, keyboard);
            if (user == null) {
                return; // the input ended before a successful login
            }
            System.out.println("Logged in as " + user
                    + ". Type a message, '/adduser' to create a user (admin only), or 'bye' to quit.");

            while (keyboard.hasNextLine()) {
                String text = keyboard.nextLine();
                if (text.equals("bye")) {
                    break;
                }
                if (text.equals("/adduser")) {
                    addUser(out, in, keyboard);
                    continue;
                }
                Response response = send(out, in, new Request(RequestType.ECHO, text));
                System.out.println(response.isSuccess() ? response.getMessage() : "Error: " + response.getMessage());
            }
        } catch (ConnectException e) {
            System.err.println("Could not connect to the server at " + Protocol.HOST + ":" + Protocol.PORT
                    + ". Is the server running?");
        } catch (EOFException e) {
            System.out.println("The server closed the connection.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not talk to the server: " + e);
        }
    }

    /** Asks for a username and password until the server accepts them. Returns null if the input ends. */
    private static User logIn(ObjectOutputStream out, ObjectInputStream in, Scanner keyboard)
            throws IOException, ClassNotFoundException {
        while (true) {
            String username = prompt(keyboard, "Username: ");
            String password = username == null ? null : prompt(keyboard, "Password: ");
            if (password == null) {
                return null;
            }

            Response response = send(out, in, new Request(RequestType.LOGIN, new Credentials(username, password)));
            if (response.isSuccess()) {
                return (User) response.getData();
            }
            System.out.println("Login failed: " + response.getMessage());
        }
    }

    /** Asks for the details of a new account and sends them to the server. */
    private static void addUser(ObjectOutputStream out, ObjectInputStream in, Scanner keyboard)
            throws IOException, ClassNotFoundException {
        String username = prompt(keyboard, "New username: ");
        String password = username == null ? null : prompt(keyboard, "New password: ");
        String roleText = password == null ? null : prompt(keyboard, "Role " + Arrays.toString(Role.values()) + ": ");
        String branch = roleText == null ? null : prompt(keyboard, "Branch: ");
        if (branch == null) {
            return; // the input ended
        }

        Role role;
        try {
            role = Role.valueOf(roleText.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Unknown role \"" + roleText + "\"");
            return;
        }

        Response response = send(out, in, new Request(RequestType.ADD_USER,
                new NewAccount(username.trim(), password, role, branch.trim())));
        System.out.println(response.isSuccess() ? response.getMessage() : "Error: " + response.getMessage());
    }

    /** Prints a prompt and reads one line. Returns null if there is no more input. */
    private static String prompt(Scanner keyboard, String text) {
        System.out.print(text);
        return keyboard.hasNextLine() ? keyboard.nextLine() : null;
    }

    /** Sends one Request and waits for the Response. */
    private static Response send(ObjectOutputStream out, ObjectInputStream in, Request request)
            throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Response) in.readObject();
    }
}
