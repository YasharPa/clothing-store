package server;

import common.Credentials;
import common.NewAccount;
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

    private UserStore store;
    private LoggedInUsers loggedInUsers;
    private ClientHandler handler;

    @BeforeEach
    void createHandlerWithTwoUsers() throws IOException {
        store = new UserStore(folder.resolve("users.txt"));
        store.addUser("dana", "pass-123", Role.CASHIER, "Tel Aviv");
        store.addUser("root", "root-pass-1", Role.ADMIN, "HQ");
        loggedInUsers = new LoggedInUsers();
        // process() does not use the socket, so the tests do not need a real connection.
        handler = new ClientHandler(null, store, loggedInUsers);
    }

    /** A second connection to the same server: its own handler, the same user list and logged-in list. */
    private ClientHandler anotherConnection() {
        return new ClientHandler(null, store, loggedInUsers);
    }

    private Response login(ClientHandler who, String username, String password) {
        return who.process(new Request(RequestType.LOGIN, new Credentials(username, password)));
    }

    private Response addUser(ClientHandler who, String username, String password, Role role, String branch) {
        return who.process(new Request(RequestType.ADD_USER, new NewAccount(username, password, role, branch)));
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
    void adminCanAddAUserWhoCanThenLogInOnAnotherConnection() {
        login("root", "root-pass-1");

        Response added = addUser(handler, "eli", "eli-pass-1", Role.SALESPERSON, "Haifa");

        assertTrue(added.isSuccess());
        // No restart in between: a different connection can use the new account straight away.
        ClientHandler eliConnection = anotherConnection();
        Response eliLogin = eliConnection.process(
                new Request(RequestType.LOGIN, new Credentials("eli", "eli-pass-1")));
        assertTrue(eliLogin.isSuccess());
        assertEquals(Role.SALESPERSON, ((User) eliLogin.getData()).getRole());
        assertEquals("Haifa", ((User) eliLogin.getData()).getBranch());
    }

    @Test
    void nonAdminCannotAddAUser() {
        login("dana", "pass-123");

        Response response = addUser(handler, "eli", "eli-pass-1", Role.SALESPERSON, "Haifa");

        assertFalse(response.isSuccess());
        assertEquals("Not allowed: only an admin can add users", response.getMessage());
        assertFalse(store.exists("eli"));
    }

    @Test
    void nobodyCanAddAUserBeforeLoggingIn() {
        Response response = addUser(handler, "eli", "eli-pass-1", Role.SALESPERSON, "Haifa");

        assertFalse(response.isSuccess());
        assertEquals("Please log in first", response.getMessage());
        assertFalse(store.exists("eli"));
    }

    @Test
    void addingAnExistingUsernameGivesAnError() {
        login("root", "root-pass-1");

        Response response = addUser(handler, "dana", "other", Role.CASHIER, "Haifa");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("already exists"));
    }

    @Test
    void addingAUserWithAnEmptyPasswordGivesAnError() {
        login("root", "root-pass-1");

        assertFalse(addUser(handler, "eli", "", Role.CASHIER, "Haifa").isSuccess());
        assertFalse(store.exists("eli"));
    }

    @Test
    void addUserWithoutAccountDetailsIsRejected() {
        login("root", "root-pass-1");

        assertFalse(handler.process(new Request(RequestType.ADD_USER, "not an account")).isSuccess());
    }

    @Test
    void sameUserCannotLogInFromASecondComputer() {
        assertTrue(login("dana", "pass-123").isSuccess());

        Response second = login(anotherConnection(), "dana", "pass-123");

        assertFalse(second.isSuccess());
        assertEquals("This user is already logged in on another computer", second.getMessage());
    }

    @Test
    void theRefusedSecondConnectionIsNotLoggedIn() {
        login("dana", "pass-123");
        ClientHandler second = anotherConnection();
        login(second, "dana", "pass-123");

        assertFalse(second.process(new Request(RequestType.ECHO, "hi")).isSuccess());
    }

    @Test
    void userCanLogInAgainAfterTheFirstConnectionEnds() {
        login("dana", "pass-123");
        handler.logOut(); // the first connection ends

        assertTrue(login(anotherConnection(), "dana", "pass-123").isSuccess());
    }

    @Test
    void afterLoggingOutTheSameConnectionIsNotLoggedInAnyMore() {
        login("dana", "pass-123");
        handler.logOut();

        assertFalse(echo("hi").isSuccess());
    }

    @Test
    void differentUsersCanBeLoggedInAtTheSameTime() {
        assertTrue(login("dana", "pass-123").isSuccess());

        assertTrue(login(anotherConnection(), "root", "root-pass-1").isSuccess());
    }

    @Test
    void aWrongPasswordGivesTheNormalErrorEvenIfTheUserIsOnline() {
        // Otherwise anyone could find out who is online without knowing a password.
        login("dana", "pass-123");

        Response response = login(anotherConnection(), "dana", "wrong");

        assertEquals("Wrong username or password", response.getMessage());
    }

    @Test
    void aFailedLoginDoesNotBlockTheRealUser() {
        login(anotherConnection(), "dana", "wrong");

        assertTrue(login("dana", "pass-123").isSuccess());
    }

    @Test
    void logoutRequestFreesTheUserForAnotherComputer() {
        login("dana", "pass-123");

        Response response = handler.process(new Request(RequestType.LOGOUT, null));

        assertTrue(response.isSuccess());
        assertFalse(loggedInUsers.isLoggedIn("dana"));
        assertFalse(echo("hi").isSuccess()); // this connection is logged out too
        assertTrue(login(anotherConnection(), "dana", "pass-123").isSuccess());
    }

    @Test
    void logoutRequestWithoutBeingLoggedInGivesAnError() {
        Response response = handler.process(new Request(RequestType.LOGOUT, null));

        assertFalse(response.isSuccess());
        assertEquals("Not logged in", response.getMessage());
    }

    @Test
    void loggingOutWhenNobodyIsLoggedInDoesNothing() {
        handler.logOut();

        assertFalse(loggedInUsers.isLoggedIn("dana"));
    }

    @Test
    void requestWithoutATypeGetsAnError() {
        Response response = handler.process(new Request(null, "hello"));

        assertFalse(response.isSuccess());
        assertEquals("The request has no type", response.getMessage());
    }
}
