# Clothing Store Chain Management System

Group project for the Java algorithmic development course (HIT, Semester A 2026).
A client-server Java app that manages a chain of clothing stores: per-branch inventory,
shared customers, employees, reports, chat between branches and logs.

**Building on this code? Read [docs/for-teammates.md](docs/for-teammates.md) first.**

## What works now
- **Client-server:** one server, many clients at once (one thread per client).
- **Login screen:** username + hidden password; passwords are stored only as salted hashes.
- **Roles:** ADMIN, SHIFT_MANAGER, CASHIER, SALESPERSON. Only an ADMIN can create users.
- **No double login:** the same user cannot be logged in on two computers at once.

## Running the program (VS Code, from the project root, JDK 21)
1. Once: run src/server/SeedUsers.java. It creates data/users.txt with 4 test accounts (usernames admin, manager, cashier, seller; the test passwords are in SeedUsers.java).
2. Once: run src/server/SeedStore.java. It creates the products and the stock of the 3 branches (data/inventory-products.txt, data/inventory-stock.txt). Run it before the server; it does nothing if those files already exist.
3. Run src/server/Server.java and leave it running.
4. Run src/client/Console.java (as many times as you like) and log in. It is the console client for customers, reports, inventory, logs and employees

### Adding users
- **Server running (normal case):** log in as an admin in the text client (`Client.java`) and type `/adduser`.
  The new user can log in immediately; nobody has to be logged out. Only an ADMIN is allowed to do this.
- **Server stopped (e.g. to create the very first admin):** run `src/server/AddUser.java` and answer its four questions.
  It refuses to run while the server is running (it checks whether the server's port is in use).

## Running the tests
- Folders: `src/` (code), `test/` (JUnit tests), `lib/` (the JUnit jar, committed so everyone has it).
- In VS Code: open the Testing panel (flask icon) and press run. `.vscode/settings.json` already points at `lib/` and `test/`.
- From a terminal (from the project root). Compile - **Windows PowerShell** (the VS Code default on Windows):
  ```
  javac -cp lib/junit-platform-console-standalone-1.13.4.jar -d out (Get-ChildItem src,test -Recurse -Filter *.java).FullName
  ```
  or **Git Bash / Mac / Linux**:
  ```
  javac -cp lib/junit-platform-console-standalone-1.13.4.jar -d out $(find src test -name "*.java")
  ```
  Then run the tests (same in every terminal):
  ```
  java -jar lib/junit-platform-console-standalone-1.13.4.jar execute --class-path out --scan-class-path
  ```

## Folder layout
- `src/common/` - classes both sides share: messages (`Request`, `Response`, ...), `Role`, `User`, `Protocol`
- `src/server/` - the server: `Server`, `ClientHandler` (one per client), `UserStore`, `PasswordHasher`, `LoggedInUsers`
- `src/client/` - the client: `ClientApp`, `LoginWindow`, `MainWindow`, `ServerConnection`, text `Client`
- `test/` - JUnit tests, same packages as `src/`
- `docs/for-teammates.md` - how the code fits together and how to add your feature
- `docs/spec-summary.md` - English summary of the assignment document
- `docs/plan.md` - the step-by-step plan used to build the client-server and login parts
- `docs/learning-notes.md` - the Java concepts used, in short
- `docs/step-0-setup.md` - installing the tools (JDK, VS Code, git)
