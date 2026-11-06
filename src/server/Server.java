package server;

import common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

/**
 * A server that accepts clients and gives each one its own worker thread.
 * Every worker shares the same UserStore (to check logins) and the same LoggedInUsers
 * (to stop a user from being logged in on two computers).
 */
public class Server {

    /** Starts the server. It runs until the program is stopped. */
    public static void main(String[] args) {
        // try-with-resources: the ServerSocket is closed automatically at the end.
        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            UserStore userStore = new UserStore(SeedUsers.USERS_FILE);
            if (!Files.exists(SeedUsers.USERS_FILE)) {
                System.out.println("Warning: " + SeedUsers.USERS_FILE
                        + " not found, so nobody can log in. Run server.SeedUsers first.");
            }
            LoggedInUsers loggedInUsers = new LoggedInUsers();
            System.out.println("Server listening on port " + Protocol.PORT);

            while (true) {
                // accept() waits here until a client connects.
                Socket socket = serverSocket.accept();
                // Hand the client to a new worker, then go straight back to waiting.
                new Thread(new ClientHandler(socket, userStore, loggedInUsers)).start();
            }
        } catch (IOException e) {
            System.err.println("Could not start the server: " + e.getMessage());
        }
    }
}
