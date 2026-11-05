# Step 0 - Setting up the machine

Goal: be able to write, compile and run a Java program on this computer.

## The three things you need (and what they are)
| Thing | What it is | Our choice |
|---|---|---|
| **JDK** | The Java toolbox: `javac` (turns your code into a runnable program) and `java` (runs it). | JDK 21 (LTS) - ask your group if they use a different version and match it |
| **IDE** | The editor you write code in. Highlights errors, runs the program with one button. | VS Code + "Extension Pack for Java" |
| **Git** | History of the files, and the way the team shares code. | Already installed (2.52) |

We are using **VS Code** (our preference). IntelliJ would also work, but everything below is for VS Code.

## 1. Get a JDK (VS Code does not bring its own)
- Install **Eclipse Temurin JDK 21** (adoptium.net, choose the Windows `.msi` installer).
- In the installer, enable the options **"Set JAVA_HOME variable"** and **"Add to PATH"**.
- Check in a new terminal: `java -version` and `javac -version` should both print 21.x.
  (Close and reopen the terminal/VS Code after installing, or it won't see the new PATH.)

## 2. Install VS Code + the Java extension
- Install VS Code (code.visualstudio.com) if you haven't already.
- Extensions panel (Ctrl+Shift+X) -> install **"Extension Pack for Java"** (by Microsoft). It includes
  the language support, debugger, and test runner.

## 3. Open this folder
- *File -> Open Folder...* -> `store-chain-project`.
- Java sources live in `src/`. For a simple project VS Code needs no build file: it compiles what's in `src`.
  (Later we may add Maven if the course asks for it.)

## 4. Check it works (hello world)
Create `src/Hello.java`:
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("It works!");
    }
}
```
A small **Run** link appears above `main` - click it (or press F5). If you see `It works!` in the terminal panel, step 0 is done.
(Delete `Hello.java` afterwards.)

## 5. Git
- Local repo is already created (`git init`) and `.gitignore` is in place.
- Later: when the group has a GitHub repo, we connect to it (`git remote add origin <url>`) and push.

## Note about OneDrive
This folder lives inside OneDrive. OneDrive syncing the hidden `.git` folder can corrupt the repo.
When we're ready to commit regularly, it's safer to move the project to something like `C:\dev\store-chain-project`.
