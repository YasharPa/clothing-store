package client;

import common.Credentials;
import common.Request;
import common.RequestType;
import common.Response;
import common.Role;
import common.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import server.ClientHandler;
import server.LoggedInUsers;
import server.UserStore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Talks to a real (small) server over a real socket, the way the windows do. */
class ServerConnectionTest {

    @TempDir
    Path folder;

    private ServerSocket serverSocket;
    private LoggedInUsers loggedInUsers;

    /** Starts a server inside the test, on a free port chosen by the computer (port 0). */
    @BeforeEach
    void startServer() throws IOException {
        UserStore store = new UserStore(folder.resolve("users.txt"));
        store.addUser("dana", "pass-123", Role.CASHIER, "Tel Aviv");
        loggedInUsers = new LoggedInUsers();
        serverSocket = new ServerSocket(0);

        Thread acceptLoop = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(new ClientHandler(socket, store, loggedInUsers)).start();
                }
            } catch (IOException e) {
                // The test closed the server socket: stop accepting.
            }
        });
        acceptLoop.setDaemon(true);
        acceptLoop.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        serverSocket.close();
    }

    private ServerConnection connect() throws IOException {
        return new ServerConnection("localhost", serverSocket.getLocalPort());
    }

    private static Request login(String username, String password) {
        return new Request(RequestType.LOGIN, new Credentials(username, password));
    }

    @Test
    void loginWorksOverARealConnection() throws IOException {
        try (ServerConnection connection = connect()) {
            Response response = connection.send(login("dana", "pass-123"));

            assertTrue(response.isSuccess());
            assertEquals(Role.CASHIER, ((User) response.getData()).getRole());
        }
    }

    @Test
    void wrongPasswordIsRefusedOverARealConnection() throws IOException {
        try (ServerConnection connection = connect()) {
            assertFalse(connection.send(login("dana", "wrong")).isSuccess());
        }
    }

    @Test
    void logoutFreesTheUserImmediately() throws IOException {
        try (ServerConnection first = connect(); ServerConnection second = connect()) {
            first.send(login("dana", "pass-123"));
            assertFalse(second.send(login("dana", "pass-123")).isSuccess());

            first.send(new Request(RequestType.LOGOUT, null));

            assertTrue(second.send(login("dana", "pass-123")).isSuccess());
        }
    }

    @Test
    void closingTheConnectionLogsTheUserOut() throws Exception {
        ServerConnection connection = connect();
        connection.send(login("dana", "pass-123"));

        connection.close();

        // The server needs a moment to notice the closed connection.
        long deadline = System.currentTimeMillis() + 3000;
        while (loggedInUsers.isLoggedIn("dana") && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(loggedInUsers.isLoggedIn("dana"));
    }

    @Test
    void connectingWhenNoServerIsRunningThrows() throws IOException {
        int freePort;
        try (ServerSocket temporary = new ServerSocket(0)) {
            freePort = temporary.getLocalPort();
        } // closed again, so nothing listens on freePort now

        assertThrows(IOException.class, () -> new ServerConnection("localhost", freePort));
    }
}
