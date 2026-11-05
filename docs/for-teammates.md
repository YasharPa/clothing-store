# For teammates: how the code fits together

This covers the parts that already exist (client-server + login) and how to add your feature on top.

## The big picture

```
 CLIENT (each employee's computer)                    SERVER (one, always running)
 ┌───────────────────────────┐                        ┌──────────────────────────────────────┐
 │ LoginWindow → MainWindow  │                        │ Server: accepts connections           │
 │        │                  │   Request  ────────►   │   └─ one ClientHandler per client      │
 │ ServerConnection.send() ──┼────────────────────────┼─►     process(request): switch on type │
 │                           │   ◄──────── Response   │        uses UserStore, LoggedInUsers   │
 └───────────────────────────┘                        └──────────────────────────────────────┘
               both sides share the classes in src/common/
```

- Every action is **one `Request` → one `Response`**.
- A `Request` has a **type** (`RequestType`) and a **payload** (any `Serializable` object).
- A `Response` has **success** (true/false), a **message** for the user, and optional **data**.
- Each connected client gets its own `ClientHandler` (its own thread). The handler remembers
  **who is logged in on that connection** in its `user` field (null = not logged in).

## Adding a new feature (example: "sell an item")

1. **Payload class in `src/common/`** (if you need to send data), e.g. `SaleDetails`. It must
   `implements Serializable` and have `private static final long serialVersionUID = 1L;`.
   If you send back data in the `Response`, that class must be `Serializable` too.
2. **Request type** in `common/RequestType.java`, with a comment saying what its payload is:
   ```java
   /** Payload: SaleDetails. Records a sale. Needs a logged-in user. */
   SELL_ITEM,
   ```
3. **Server side** in `server/ClientHandler.java`: add a case to the switch in `process()`
   (Java will refuse to compile until you do) and a method that follows the usual pattern:
   ```java
   case SELL_ITEM -> sellItem(request);
   ...
   private Response sellItem(Request request) {
       if (user == null) {                                   // 1. logged in?
           return new Response(false, "Please log in first");
       }
       if (user.getRole() == Role.ADMIN) {                   // 2. allowed for this role? (your rule)
           return new Response(false, "Not allowed");
       }
       if (!(request.getPayload() instanceof SaleDetails sale)) {   // 3. right kind of data?
           return new Response(false, "A sale needs its details");
       }
       // 4. do it - use user.getBranch() to work only on this user's branch
       return new Response(true, "Sale recorded");
   }
   ```
   Put the real work in its own class (like `UserStore`), not inside `ClientHandler`.
   If that class is shared by all handlers, it must be **thread-safe** (`ConcurrentHashMap`, `synchronized`).
4. **Client side:** send it through the window's `ServerConnection`, **in a `SwingWorker`**
   so the window never freezes (copy the pattern from `LoginWindow.logIn()` or `MainWindow.logOut()`):
   ```java
   Response response = connection.send(new Request(RequestType.SELL_ITEM, new SaleDetails(...)));
   ```
5. **Tests** in `test/server/ClientHandlerTest.java` (call `handler.process(...)` directly, no network needed),
   plus tests for your own classes.

## Where your screens go
`client/MainWindow` is a placeholder that shows who is logged in and has a Log out button.
Add your screens there (for example tabs, or buttons that open them). It already holds the logged-in
`ServerConnection` and the `User` (name, role, branch).

## Rules the existing code follows (please keep them)
- **The server decides.** Never trust the client for logins, roles or branch checks.
- **Never log or store a plain password.** The server logs only the request *type* and usernames.
- **Same error for a wrong username and a wrong password**, so nobody can find out which users exist.
- **A problem with one client must not stop the server** (catch the error, answer with a failed Response).

## Things that are not decided yet (ask the group)
- **Branches** are plain text (`"Tel Aviv"`). A fixed list or a `Branch` class may be better for inventory.
- **The admin screen** (create employee accounts) is not built. The server side exists:
  send `Request(ADD_USER, new NewAccount(username, password, role, branch))` as a logged-in ADMIN.
- **Customers** are records, not users; there are no customer logins.
- The windows use **Swing**.

## Known limits
- If a computer loses its connection silently (cable pulled), the server only notices when the operating
  system times out the connection, so that user may stay "logged in" for a while.
- There is no password reset yet: a forgotten password needs a new account.
- `data/users.txt` is git-ignored; each of us creates our own with `SeedUsers`.
