# QuizNet - Real-Time Multiplayer Quiz System (Plain Java project)

## Overview
This is a simple Java project demonstrating Java network programming concepts:
- Java NIO (Selector/Channels)
- TCP sockets
- Multi-threading

It contains a NIO-based server and a Java console client. Questions are read from `questions.txt`.

## Structure
QuizNet/
├── README.md
├── questions.txt
├── server/
│   ├── QuizServer.java
│   ├── QuestionManager.java
│   └── ScoringEngine.java
└── client/
    ├── QuizClient.java
    └── ClientListener.java

## Build & Run (simple, using javac/java)

1. Ensure JDK 11+ is installed and `javac` on PATH.

2. Compile server:
   ```
   javac -d out/server server/*.java
   ```

3. Run server (from project root):
   ```
   java -cp out/server QuizServer
   ```

4. In another terminal, compile client:
   ```
   javac -d out/client client/*.java
   ```

5. Run client (multiple clients allowed):
   ```
   java -cp out/client QuizClient
   ```

## How to use
- Client will prompt for nickname.
- Any client can type `start` to begin the quiz (wait 3s).
- During quiz, server broadcasts `QUESTION|Qid|...`
- To answer type: `answer Q0 2` (example) to send answer for question Q0 option 2.
- To chat: `chat Hello everyone`
- To quit: type `quit`

## Notes
- This project intentionally uses a single quiz room (all connected clients participate).
- Scores reset each server run.
- Questions are loaded from `questions.txt` in the root project directory.
