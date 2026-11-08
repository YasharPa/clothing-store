package server;

import common.Role;
import common.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStoreTest {

    @TempDir
    Path folder; // JUnit gives each test its own empty temporary folder

    private Path file;
    private UserStore store;

    @BeforeEach
    void createStoreWithOneUser() throws IOException {
        file = folder.resolve("users.txt");
        store = new UserStore(file);
        store.addUser("dana", "pass-123", Role.CASHIER, "Tel Aviv");
    }

    @Test
    void rightPasswordReturnsTheUser() {
        Optional<User> user = store.authenticate("dana", "pass-123");

        assertTrue(user.isPresent());
        assertEquals("dana", user.get().getUsername());
        assertEquals(Role.CASHIER, user.get().getRole());
        assertEquals("Tel Aviv", user.get().getBranch());
    }

    @Test
    void wrongPasswordIsRejected() {
        assertTrue(store.authenticate("dana", "wrong").isEmpty());
    }

    @Test
    void unknownUserIsRejected() {
        assertTrue(store.authenticate("nobody", "pass-123").isEmpty());
    }

    @Test
    void nullInputIsRejectedWithoutCrashing() {
        assertTrue(store.authenticate(null, "pass-123").isEmpty());
        assertTrue(store.authenticate("dana", null).isEmpty());
    }

    @Test
    void theFileDoesNotContainThePlainPassword() throws IOException {
        String content = Files.readString(file);

        assertFalse(content.contains("pass-123"));
        assertTrue(content.contains("dana"));
    }

    @Test
    void usersSurviveReloadingTheFile() throws IOException {
        UserStore reloaded = new UserStore(file);

        assertTrue(reloaded.authenticate("dana", "pass-123").isPresent());
        assertTrue(reloaded.authenticate("dana", "wrong").isEmpty());
    }

    @Test
    void duplicateUsernameIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> store.addUser("dana", "other", Role.SALESPERSON, "Haifa"));
    }

    @Test
    void fieldsThatWouldBreakTheFileAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> store.addUser("bad;name", "pw", Role.CASHIER, "Haifa"));
        assertThrows(IllegalArgumentException.class,
                () -> store.addUser("eli", "", Role.CASHIER, "Haifa"));
    }

    @Test
    void aUserWhoCouldNotBeSavedIsNotKeptInMemory() throws IOException {
        // The "folder" of this file is really a normal file, so saving must fail.
        Path notAFolder = folder.resolve("blocker");
        Files.writeString(notAFolder, "I am a file");
        UserStore failing = new UserStore(notAFolder.resolve("users.txt"));

        assertThrows(IOException.class, () -> failing.addUser("eli", "pw", Role.CASHIER, "Haifa"));

        assertFalse(failing.exists("eli"));
        assertTrue(failing.authenticate("eli", "pw").isEmpty());
    }

    @Test
    void aBrokenFileIsReportedInsteadOfIgnored() throws IOException {
        Path broken = folder.resolve("broken.txt");
        Files.writeString(broken, "only;three;parts\n");

        assertThrows(IOException.class, () -> new UserStore(broken));
    }
}
