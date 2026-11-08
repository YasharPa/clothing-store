package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Turns passwords into hashes, so the real password is never stored.
 *
 * A hash is a one-way scramble: the same input always gives the same hash,
 * but you cannot get the password back from the hash.
 * A "salt" is a random value mixed in first, so two users with the same
 * password still get different hashes.
 */
public final class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    // Nobody should create a PasswordHasher object; it only holds helper methods.
    private PasswordHasher() {
    }

    /** Creates a new random salt, as text. */
    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Hashes the password together with the salt (SHA-256), and returns the result as text. */
    public static String hash(String password, String salt) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(Base64.getDecoder().decode(salt));
            byte[] digest = sha256.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every Java installation has SHA-256, so this cannot really happen.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Checks a typed password against the stored hash. */
    public static boolean matches(String password, String salt, String expectedHash) {
        byte[] attempt = hash(password, salt).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHash.getBytes(StandardCharsets.UTF_8);
        // isEqual takes the same time however early the two differ (safer than equals).
        return MessageDigest.isEqual(attempt, expected);
    }
}
