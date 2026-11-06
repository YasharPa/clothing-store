# Learning notes

The concepts used in the client-server and login parts, in short. Good for revising before the exam.

## Client-server
- **Server** waits for connections; **client** connects and asks. The server holds the data and decides.
- **IP/host + port** = which computer + which program on it. `localhost` = this computer; we use port 5000.
- **Socket** = the "phone line". `ServerSocket.accept()` waits for a client; `new Socket(host, port)` calls the server.
- **Streams**: `in` (what the other side says) and `out` (what we say). `flush()` = send now.
- **Request/response**: the client sends a `Request`, the server answers with a `Response`. One request, one answer.

## Objects over the network
- **`Serializable`** = permission to turn an object into bytes and rebuild it on the other side.
  Without it: `NotSerializableException`.
- `ObjectOutputStream.writeObject(...)` / `ObjectInputStream.readObject()` send and receive whole objects.
- Create the **output stream before the input stream** on both sides, or both wait forever (a deadlock).
- **Cast** `(Response) in.readObject()`: `readObject` returns `Object`; we say what it really is.
- **`enum`** (`RequestType`, `Role`) = a fixed list of values; a typo becomes a compile error.

## Threads
- **One thread per client** (`ClientHandler implements Runnable`, `new Thread(handler).start()`),
  so one slow client never blocks the others.
- `start()` returns at once; the new thread runs `run()` in parallel.
- **Shared data** used by many threads must be thread-safe: `ConcurrentHashMap`, `synchronized` methods.
- **Race condition**: two threads "check, then act" at the same moment. Fix: do both in one step
  (`set.add(x)` returns false if `x` was already there).
- An exception inside a thread only ends that thread, so catch it there.

## Errors
- **try / catch**: try something that may fail; catch the problem and handle it instead of crashing.
- **try-with-resources** `try (Socket s = ...)`: closes the resource automatically, even after an error.
- **finally**: runs however the block ended. We use it to log the user out whatever happened.
- `EOFException` = the other side hung up normally; `SocketException` = the connection broke.

## Passwords and security
- **Authentication** = who are you? (login). **Authorization** = what may you do? (role check).
- **Hash** = one-way scramble (SHA-256). The same input always gives the same hash; you cannot go back.
- **Salt** = random value mixed in before hashing, stored next to the hash. Same password, different hashes.
- **Checking a login**: hash the typed password with the stored salt and compare with the stored hash.
  The server never decrypts anything and never stores the password.
- `MessageDigest.isEqual` compares in constant time (no timing hints for attackers).
- Same message for a wrong username and a wrong password, so nobody learns which users exist.
- The server decides everything; the client can be changed by anyone.

## Java features we used
- `Optional<User>` = "maybe a User, maybe nothing" instead of `null`.
- `record` = a short class that only holds values (`StoredUser`).
- `instanceof Credentials c` = check the type and name it in one step.
- `switch` with `->` on an enum: Java checks every value is handled.
- Lambda `event -> logIn()` = a small function passed to be run later.

## Swing (the login window)
- `JFrame` = window; `JTextField`, `JPasswordField`, `JButton`, `JLabel` = its parts.
- **Layouts** place components: `GridLayout` (a grid), `BorderLayout` (north, south, center...).
- **Event-driven**: `addActionListener(...)` says what to do *when* something happens.
- Swing has **one thread** for the window. Slow work (the network) goes in a **`SwingWorker`**:
  `doInBackground()` runs in the background, `done()` comes back to the window thread to update it.
  Otherwise the window freezes ("Not Responding").

## Testing (JUnit)
- `@Test` methods; `assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`.
- `@BeforeEach` runs before every test; `@TempDir` gives each test an empty folder.
- A test is only useful if it can fail: break the code on purpose once and watch it turn red.
