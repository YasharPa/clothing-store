# Clothing Store Chain Management System

Group project for the Java algorithmic development course (HIT, Semester A 2026).
A client-server Java app that manages a chain of clothing stores: per-branch inventory,
shared customers, employees, reports, chat between branches and logs.

## Folder layout
- `docs/spec-summary.md` - English summary of the assignment document
- `docs/step-0-setup.md` - installing the tools (JDK, IDE, git)
- `docs/plan.md` - step-by-step plan for my two tasks (client-server, login)
- `docs/my-tasks.md` - my part of the project and my progress
- `docs/learning-notes.md` - Java refreshers as we go
- `src/` - the Java source code (will be filled in as we build)

## Running the program (VS Code, from the project root)
1. Once: run `src/server/SeedUsers.java`. It creates `data/users.txt` with 4 test accounts
   (usernames `admin`, `manager`, `cashier`, `seller`; the test passwords are in `SeedUsers.java`).
2. Run `src/server/Server.java` and leave it running.
3. Run `src/client/Client.java` (as many times as you like), log in, type a message, `bye` to quit.

### Adding users
- **Server running (normal case):** log in as an `admin` in the client and type `/adduser`. The new user can log in
  immediately; nobody has to be logged out. Only an ADMIN is allowed to do this.
- **Server stopped (e.g. to create the very first admin):** run `src/server/AddUser.java` and answer its four questions.
  Never use it while the server is running.

## Running the tests
- Folders: `src/` (code), `test/` (JUnit tests), `lib/` (the JUnit jar, committed so everyone has it).
- In VS Code: open the Testing panel (flask icon) and press run. `.vscode/settings.json` already points at `lib/` and `test/`.
- From a terminal (from the project root, JDK 21 installed):
  ```
  javac -cp lib/junit-platform-console-standalone-1.13.4.jar -d out src/common/*.java src/server/*.java src/client/*.java test/common/*.java test/server/*.java
  java -jar lib/junit-platform-console-standalone-1.13.4.jar execute --class-path out --scan-class-path
  ```

## Status
Step 0 (setup): local git repo created. Next: install JDK 21 + VS Code Java extension, see `docs/step-0-setup.md`.
