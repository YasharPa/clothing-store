# Spec summary (English)

Source: "Java Algorithmic Development" course, HIT Holon Institute of Technology, Semester A 2026. Lecturer: Roy Zimon.
Original (Hebrew) file: `פרוייקט סיכום בגאווה (1)(1).docx`

## Course rules (the "preface")
- All code (classes, interfaces...) must follow OOP principles and the course's principles: **efficient, clean, well documented**.
- On request we'll also have to write **tests** (unit tests) for our code.
- The lecturer will introduce extra programming principles / tools / methods during the semester. Learning these is **mandatory** and we can be **examined on them**.

## What the system is
A management system for a **chain of clothing stores**:
- **Separate inventory per branch**, updated live for every employee of that branch after each action.
- **Customer list** shared by the whole chain, updated live for everyone after each action.
- **Sales statistics per branch** (employees only see their own branch), with daily reports by branch, product, or product category.
- **Employee data** management.
- **1-on-1 chat between branches** (to whichever employee is free). After the chat is connected, the server is no longer involved but still knows users X and Y are busy.
- **No double login**: one user cannot be logged in from multiple computers.

## Requirements
1. **Client-server** architecture.
2. **Login screen** with user authentication.
3. **Admin screen**: create employee accounts (details + password policy).
4. Each user sees data for **their own branch** only, depending on user type.
5. **Inventory interface** (per branch): show what's in stock, allow **buying and selling** products.
6. **Customer management interface** (shared across all branches), showing full details:
   - full name, ID number (ת.ז), phone, customer type (**New / Returning / VIP**)
   - each customer type has its own **purchase track** (promotions)
   - **a different class for each customer type**, each handling its own promotion details
   - the purchase action applies buy/sell according to the customer's track (i.e. polymorphism)
7. **Reports interface**:
   - number of sales per branch
   - sales report by specific product(s) or category(ies)
   - reports must be viewable in a **Notepad document** (text file)
8. **Employee management interface**: full name, ID, phone, bank account number, branch, employee number, role (**shift manager, cashier, salesperson**).
9. **Chat management interface**:
   - an employee in one branch can open a chat with an employee in another branch
   - a shift manager can **join an existing chat**
   - a **queue** looks for a free employee; if one is free, the chat opens
   - if nobody is free, the server keeps a list of users who didn't get a call; when someone becomes free they get a notification and can chat back with whoever asked
   - must use a **Design Pattern from the self-study material**, plus queue management / call handling
   - same user can't chat from multiple computers at once
10. **System logs interface**, logging by action type:
    - employee registration
    - customer registration
    - product purchases / sales
    - chat details between branches, with an option to save the chat content

## Open questions (not answered by the document)
- Which tasks are "task 1" and "task 2"? (the document has no task list)
- Which design pattern will our group use for the chat/queue?
- Storage: files, or a database? (document doesn't say)
- GUI: Swing, JavaFX, or console? (document doesn't say)
