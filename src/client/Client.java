package client;

import common.Credentials;
import common.Protocol;
import common.Request;
import common.RequestType;
import common.Response;
import common.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Step 8: a client that connects to the server, asks you to log in,
 * and then sends what you type as ECHO requests. Type "bye" to quit.
 * (The login window replaces the typed prompts in Step 10.)
 */
public class Client {

    // "localhost" = this same computer. Later this becomes the server's real address.
    private static final String HOST = "localhost";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, Protocol.PORT);
             // Output stream first, then input stream (see the note in ClientHandler.handleClient).
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner keyboard = new Scanner(System.in)) {

            System.out.println("Connected to the server.");

            User user = logIn(out, in, keyboard);
            if (user == null) {
                return; // the input ended before a successful login
            }
            System.out.println("Logged in as " + user + ". Type a message, or 'bye' to quit.");

            while (keyboard.hasNextLine()) {
                String text = keyboard.nextLine();
                if (text.equals("bye")) {
                    break;
                }
                Response response = send(out, in, new Request(RequestType.ECHO, text));
                System.out.println(response.isSuccess() ? response.getMessage() : "Error: " + response.getMessage());
            }
        } catch (ConnectException e) {
            System.err.println("Could not connect to the server at " + HOST + ":" + Protocol.PORT
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
            System.out.print("Username: ");
            if (!keyboard.hasNextLine()) {
                return null;
            }
            String username = keyboard.nextLine();
            System.out.print("Password: ");
            if (!keyboard.hasNextLine()) {
                return null;
            }
            String password = keyboard.nextLine();

            Response response = send(out, in, new Request(RequestType.LOGIN, new Credentials(username, password)));
            if (response.isSuccess()) {
                return (User) response.getData();
            }
            System.out.println("Login failed: " + response.getMessage());
        }
    }

    /** Sends one Request and waits for the Response. */
    private static Response send(ObjectOutputStream out, ObjectInputStream in, Request request)
            throws IOException, ClassNotFoundException {
        out.writeObject(request);
        out.flush();
        return (Response) in.readObject();
    }
}
