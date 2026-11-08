package server;

import common.Role;
import common.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's list of users, kept in a text file.
 *
 * Each line of the file is:  username;salt;passwordHash;ROLE;branch
 * The file never contains a plain password.
 */
public class UserStore {

    private static final String SEPARATOR = ";";

    /** What we keep for each user: the safe-to-share User plus the secret salt and hash. */
    private record StoredUser(String salt, String passwordHash, User user) {
    }

    private final Path file;
    // Several client threads can read at the same time, so we use a thread-safe map.
    private final Map<String, StoredUser> users = new ConcurrentHashMap<>();

    /** Opens the store. If the file does not exist yet, the store starts empty. */
    public UserStore(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) {
            load();
        }
    }

    /**
     * Checks a login.
     *
     * @return the User if the username and password are right, otherwise empty
     */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        StoredUser stored = users.get(username);
        if (stored == null) {
            return Optional.empty();
        }
        if (PasswordHasher.matches(password, stored.salt(), stored.passwordHash())) {
            return Optional.of(stored.user());
        }
        return Optional.empty();
    }

    /**
     * Adds a new user and saves the file.
     *
     * @throws IllegalArgumentException if the username is taken or a field is not allowed
     */
    public synchronized void addUser(String username, String password, Role role, String branch)
            throws IOException {
        checkField("username", username);
        checkField("branch", branch);
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("username already exists: " + username);
        }

        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        users.put(username, new StoredUser(salt, hash, new User(username, role, branch)));
        try {
            save();
        } catch (IOException e) {
            // Do not keep a user in memory that is not in the file: it would vanish at the next restart.
            users.remove(username);
            throw e;
        }
    }

    /** True if a user with this name exists. */
    public boolean exists(String username) {
        return users.containsKey(username);
    }

    private static void checkField(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (value.contains(SEPARATOR) || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(name + " must not contain ';' or a line break");
        }
    }

    private void load() throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(SEPARATOR, -1);
            if (parts.length != 5) {
                throw new IOException("Bad line " + (i + 1) + " in " + file);
            }
            try {
                User user = new User(parts[0], Role.valueOf(parts[3]), parts[4]);
                users.put(parts[0], new StoredUser(parts[1], parts[2], user));
            } catch (IllegalArgumentException e) {
                throw new IOException("Unknown role on line " + (i + 1) + " in " + file, e);
            }
        }
    }

    private void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (StoredUser stored : users.values()) {
            User user = stored.user();
            lines.add(String.join(SEPARATOR, user.getUsername(), stored.salt(), stored.passwordHash(),
                    user.getRole().name(), user.getBranch()));
        }
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
