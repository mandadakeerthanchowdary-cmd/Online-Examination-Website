# Online Examination Website

A working academic project built with HTML, CSS, JavaScript and Java.

## Features
- Student login using name and student ID
- Exam instructions page
- Multiple-choice questions loaded from the Java backend
- Five-minute countdown timer with automatic submission at zero
- Manual submission with confirmation
- Server-side score calculation and result display
- Responsive layout for desktop and mobile

## Requirements
- Java JDK 11 or later
- A modern web browser

## Run the project
1. Extract the ZIP file.
2. Open a terminal/Command Prompt in the extracted `online-examination-website` folder.
3. Compile the Java server:

   **Windows**
   ```bash
   javac --add-modules jdk.httpserver -d out src/OnlineExamServer.java
   java --add-modules jdk.httpserver -cp out OnlineExamServer
   ```

   **macOS / Linux**
   ```bash
   javac --add-modules jdk.httpserver -d out src/OnlineExamServer.java
   java --add-modules jdk.httpserver -cp out OnlineExamServer
   ```

4. Open `http://localhost:8080` in your browser.
5. Enter any student name and ID, read the instructions, and start the exam.

Keep the terminal/server running while using the website. Press Ctrl+C to stop it.

## Project structure
- `src/OnlineExamServer.java` — Java HTTP server, login endpoint, question endpoint and result calculation
- `public/index.html` — website pages and structure
- `public/style.css` — styling and responsive layout
- `public/script.js` — login, exam rendering, timer, submission and result display

## Testing checklist
- Login with both fields filled: should open instructions.
- Leave a login field empty: browser validation should prevent submission.
- Start the exam: five MCQs should load and timer should show 05:00.
- Select answers: progress count should update.
- Submit before time ends: confirmation appears and result is displayed.
- Wait for timer to reach 00:00: exam should submit automatically.
- Restart: returns to login page.

## Notes
This is a demonstration/college assignment project. Login accepts any non-empty student name and ID; it is not connected to a student database. Questions and correct answers are sample Web Technology questions. Correct answers are stored in the Java server and are not sent to the browser.
