# My tasks

Both come from the "Project requirements" section of the assignment.

## Task 1 - Client-server system
Original: "המערכת הינה מערכת מבוססת קליינט סרבר"
- The whole system is split into a **server** (one program that holds the data and the rules) and **clients** (programs on employees' computers that talk to it over the network).
- Everything else in the project (inventory, customers, chat, logs...) will plug into this.
- Status: not started

## Task 2 - Login screen with user authentication
Original: "מסך חיבור עם ממשק אימות משתמש"
- A login screen on the client: user types username + password.
- The server checks them and answers "ok (and who you are / which branch / which role)" or "wrong".
- Related requirement (probably another teammate, but our login must support it): no double login of the same user from two computers.
- Status: not started

## How the two fit together
Task 1 is the "pipe" between client and server; Task 2 is the first thing that travels through it
(login request -> server checks -> reply). The login is the natural first feature to prove the pipe works.

## Open decisions (see chat)
- GUI technology (Swing / JavaFX / console first)
- Network technology (plain sockets / other, whatever the course teaches)
- Where users are stored (file / database)
- How to agree with teammates on the message format between client and server
