package server;

import common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Step 4: a server that accepts clients and gives each one its own worker thread,
 * so many clients can be served at the same time.
 */
public class Server {

    public static void main(String[] args) {
        // try-with-resources: the ServerSocket is closed automatically at the end.
        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("Server listening on port " + Protocol.PORT);

            while (true) {
                // accept() waits here until a client connects.
                Socket socket = serverSocket.accept();
                // Hand the client to a new worker, then go straight back to waiting.
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Could not start the server: " + e.getMessage());
        }
    }
}
