# Plan: Task 1 (client-server) and Task 2 (login)

Each step has: **Concepts** (what you learn), **We build** (what changes in the project), **Done when** (how we know).
We go one step at a time. Nothing in a step is written before you understand why.

## Proposed technology (to approve in Step 1)
| Choice | Why |
|---|---|
| Java 21, plain `java.net` **sockets** | The classic way to do client-server in Java; no extra libraries |
| Messages are Java **objects** (`Request` / `Response`) sent with `ObjectOutputStream` | Fits the OOP style the course wants; no text parsing |
| **Swing** for the login window | Built into Java; no install |
| Users stored in a **file** on the server (passwords hashed) | Simple; a database can replace it later |
| **JUnit** for tests | The course asks for test files |

Package layout inside `src/`:
- `common/` - classes both sides share (messages, enums). Teammates will add to this.
- `server/` - everything that runs on the server
- `client/` - everything that runs on an employee's computer

---

## TASK 1 - Client-server system

### Step 1 - Understand the idea and agree on the design  (DONE)
- **Concepts:** client vs server, request/response, IP address and port, socket, stream, protocol.
- **We build:** nothing yet. We agree on the technology table above and the package layout.
- **Done when:** you can explain in your own words what happens when a client "logs in" to a server, and you approve the design.

### Step 2 - A server that listens, a client that connects (echo)  (DONE)
- **Concepts:** `ServerSocket`, `Socket`, `accept()`, reading/writing a stream, running two programs at once.
- **We build:** `server/Server.java` and `client/Client.java`. Client sends a text, server sends it back.
- **Done when:** you start the server, start the client, and see your message come back.

### Step 3 - Real messages instead of plain text  (DONE)
- **Concepts:** classes as messages, `Serializable`, enums for message types, the shared `common` package.
- **We build:** `common/Request`, `common/Response`, `common/RequestType`. Client sends a `Request`, server replies with a `Response`.
- **Done when:** the echo now works with objects.

### Step 4 - Many clients at once  (DONE)
- **Concepts:** threads, why one slow client must not block the others, one handler per connection.
- **We build:** `server/ClientHandler` (one thread per connected client).
- **Done when:** two clients connect at the same time and both get answers.

### Step 5 - Tidy up: errors, tests  (DONE)
- **Concepts:** try-with-resources, exceptions, a client that disconnects suddenly, JUnit basics.
- **We build:**
  - 5a (DONE): the server answers wrong/empty messages with an error instead of crashing, tells a lost connection
    from a normal goodbye, and the client says "Is the server running?" when it cannot connect.
  - 5b (DONE): first JUnit tests in `test/` (JUnit jar in `lib/`, VS Code settings in `.vscode/settings.json`).
  - (Dropped: "clean shutdown" - the OS already closes connections when the program stops.)
- **Done when:** killing a client does not crash the server, and tests pass.

---

## TASK 2 - Login screen with user authentication

### Step 6 - What is authentication?  (DONE)
- **Concepts:** authentication vs authorization, why never to store plain passwords, hashing, roles and branch.
- **We build:** `common/Role` (enum: ADMIN, SHIFT_MANAGER, CASHIER, SALESPERSON - customers are records, not users),
  `common/User` data (username, role, branch), design the login request/response.
  We only *provide* the role; what each role may do is up to each feature (teammates just check `user.getRole()`).
- **Done when:** you understand why the server (not the client) must decide if a login is valid.

### Step 7 - Users on the server  (DONE, explaining the code)
- Built: `server/PasswordHasher` (salt + SHA-256), `server/UserStore` (file `data/users.txt`), `server/SeedUsers` (test accounts), tests.
- Run `server.SeedUsers` once from the project root to create `data/users.txt` (it is git-ignored).
- Known limit: one round of SHA-256 is fine for a course project but fast to brute-force; real systems use a slow
  hash (bcrypt/PBKDF2). `PasswordHasher` is the only place to change if we ever want that.
- **Concepts:** file reading/writing, hashing with `MessageDigest` (SHA-256 + salt), `Map`.
- **We build:** `server/UserStore` (loads users from a file) and a first admin user for testing.
- **Done when:** the server can answer "is this username/password valid?" in a unit test.

### Step 8 - Login over the network  (DONE, explaining the code)
- Built: `LOGIN` request type, `common/Credentials`, `Request`/`Response` now carry a payload/data,
  `ClientHandler` checks logins with `UserStore` and remembers the logged-in `User`; everything except LOGIN is refused
  until you log in; `Client` asks for username and password first.
- Changed from the original plan: no separate `AuthService` yet (it would only pass calls through). Step 9 adds
  the "who is logged in" registry, which is where a shared service becomes worth it.
- **Concepts:** switch/dispatch on request type, returning success or an error message.
- **We build:** `server/AuthService`, hook `LOGIN` into `ClientHandler`.
- **Done when:** a test client sends a login and gets "ok + role + branch" or "wrong password".

### Step 8b - Create users while the server runs  (DONE)
- Problem found while testing: adding a user needed a server restart (the server reads `users.txt` once and
  owns the file). Fix: accounts are created through the server.
- Built: `ADD_USER` request + `common/NewAccount`; `ClientHandler` allows it only for a logged-in ADMIN
  (first use of roles for authorization); `Client` has a `/adduser` command; `UserStore.addUser` no longer keeps a
  user in memory if saving the file fails. `AddUser.java` is now only for creating the first admin while the server is stopped.
- For the teammate doing the admin screen: send `Request(ADD_USER, new NewAccount(...))` and show the `Response` message.

### Step 9 - No double login  <- next
- **Concepts:** shared state between threads, `ConcurrentHashMap`/`synchronized`, logging out when the connection drops.
- **We build:** a set of currently-logged-in users on the server.
- **Done when:** the second login of the same user is refused; after the first disconnects, it's allowed again.

### Step 10 - The login window
- **Concepts:** Swing (`JFrame`, `JTextField`, `JPasswordField`, `JButton`), event listeners, never freezing the window while waiting for the server.
- **We build:** `client/LoginWindow`.
- **Done when:** you type a username and password, press Login, and see success or an error message.

### Step 11 - Polish and hand-off
- **Concepts:** documentation (Javadoc), clean code, what teammates need from our part.
- **We build:** Javadoc on public classes, a short "how to run" in the README, more tests.
- **Done when:** a teammate can pull the repo, run the server and client, and log in.
