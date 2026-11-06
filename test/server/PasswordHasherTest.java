package server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void samePasswordAndSaltGiveTheSameHash() {
        String salt = PasswordHasher.newSalt();

        assertEquals(PasswordHasher.hash("secret", salt), PasswordHasher.hash("secret", salt));
    }

    @Test
    void differentSaltsGiveDifferentHashesForTheSamePassword() {
        String hash1 = PasswordHasher.hash("secret", PasswordHasher.newSalt());
        String hash2 = PasswordHasher.hash("secret", PasswordHasher.newSalt());

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashDoesNotContainThePassword() {
        String hash = PasswordHasher.hash("secret", PasswordHasher.newSalt());

        assertFalse(hash.contains("secret"));
    }

    @Test
    void matchesAcceptsTheRightPasswordAndRejectsAWrongOne() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret", salt);

        assertTrue(PasswordHasher.matches("secret", salt, hash));
        assertFalse(PasswordHasher.matches("Secret", salt, hash));
    }
}
