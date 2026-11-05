package server;

import common.Credentials;
import common.Request;
import common.RequestType;
import common.Response;
import common.Role;
import common.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientHandlerTest {

    @TempDir
    Path folder;

    private ClientHandler handler;

    @BeforeEach
    void createHandlerWithOneUser() throws IOException {
        UserStore store = new UserStore(folder.resolve("users.txt"));
        store.addUser("dana", "pass-123", Role.CASHIER, "Tel Aviv");
        // process() does not use the socket, so the tests do not need a real connection.
        handler = new ClientHandler(null, store);
    }

    private Response login(String username, String password) {
        return handler.process(new Request(RequestType.LOGIN, new Credentials(username, password)));
    }

    private Response echo(String text) {
        return handler.process(new Request(RequestType.ECHO, text));
    }

    @Test
    void rightLoginReturnsTheUser() {
        Response response = login("dana", "pass-123");

        assertTrue(response.isSuccess());
        User user = (User) response.getData();
        assertEquals("dana", user.getUsername());
        assertEquals(Role.CASHIER, user.getRole());
        assertEquals("Tel Aviv", user.getBranch());
    }

    @Test
    void wrongPasswordIsRejected() {
        Response response = login("dana", "wrong");

        assertFalse(response.isSuccess());
        assertEquals("Wrong username or password", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void unknownUserGetsTheSameMessageAsAWrongPassword() {
        // If the messages differed, an attacker could find out which usernames exist.
        assertEquals(login("dana", "wrong").getMessage(), login("nobody", "pass-123").getMessage());
    }

    @Test
    void loginWithoutCredentialsIsRejected() {
        Response response = handler.process(new Request(RequestType.LOGIN, "not credentials"));

        assertFalse(response.isSuccess());
    }

    @Test
    void secondLoginOnTheSameConnectionIsRefused() {
        login("dana", "pass-123");

        Response second = login("dana", "pass-123");

        assertFalse(second.isSuccess());
        assertEquals("Already logged in", second.getMessage());
    }

    @Test
    void echoIsRefusedBeforeLoggingIn() {
        Response response = echo("hello");

        assertFalse(response.isSuccess());
        assertEquals("Please log in first", response.getMessage());
    }

    @Test
    void echoWorksAfterLoggingIn() {
        login("dana", "pass-123");

        Response response = echo("hello");

        assertTrue(response.isSuccess());
        assertEquals("Echo: hello", response.getMessage());
    }

    @Test
    void failedLoginDoesNotLetYouIn() {
        login("dana", "wrong");

        assertFalse(echo("hello").isSuccess());
    }

    @Test
    void echoWithoutTextIsRejected() {
        login("dana", "pass-123");

        assertFalse(handler.process(new Request(RequestType.ECHO, null)).isSuccess());
    }

    @Test
    void requestWithoutATypeGetsAnError() {
        Response response = handler.process(new Request(null, "hello"));

        assertFalse(response.isSuccess());
        assertEquals("The request has no type", response.getMessage());
    }
}
