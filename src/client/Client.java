package client;

import common.Protocol;
import common.Request;
import common.RequestType;
import common.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Step 3: a client that connects to the server, sends what you type as a Request,
 * and prints the Response. Type "bye" to quit.
 */
public class Client {

    // "localhost" = this same computer. Later this becomes the server's real address.
    private static final String HOST = "localhost";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, Protocol.PORT);
             // Output stream first, then input stream (see the note in Server.handleClient).
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             Scanner keyboard = new Scanner(System.in)) {

            System.out.println("Connected to the server. Type a message, or 'bye' to quit.");

            while (keyboard.hasNextLine()) {
                String text = keyboard.nextLine();
                if (text.equals("bye")) {
                    break;
                }

                out.writeObject(new Request(RequestType.ECHO, text));  // send a Request
                out.flush();
                Response response = (Response) in.readObject();        // wait for the Response

                if (response.isSuccess()) {
                    System.out.println(response.getMessage());
                } else {
                    System.out.println("Error: " + response.getMessage());
                }
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
}
