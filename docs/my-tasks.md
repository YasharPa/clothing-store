# My tasks

Both come from the "Project requirements" section of the assignment.

## Task 1 - Client-server system  (DONE)
Original: "המערכת הינה מערכת מבוססת קליינט סרבר"
- One server, many clients at the same time (one thread per client).
- Clients and server talk with `Request` / `Response` objects defined in `src/common/`.
- Bad messages and broken connections do not crash the server.

## Task 2 - Login screen with user authentication  (DONE)
Original: "מסך חיבור עם ממשק אימות משתמש"
- Login window (Swing): username, hidden password, clear error messages, never freezes.
- The server checks logins against `data/users.txt`, which stores only salted SHA-256 hashes.
- Roles (ADMIN, SHIFT_MANAGER, CASHIER, SALESPERSON) and branch come back with a successful login.
- Extra, needed by other requirements:
  - no double login (also a separate requirement in the assignment),
  - an admin can create users while the server runs (`ADD_USER`), for the admin screen,
  - log out.

## How they were built
See `docs/plan.md` (step by step) and `docs/learning-notes.md` (the concepts).

## Still open (group decisions, not code)
- Swing for everyone's screens? Who builds the admin screen? Branch list? Customer logins?
- Shared GitHub repository.
