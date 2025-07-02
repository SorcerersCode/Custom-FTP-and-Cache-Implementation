# Custom TCP and Stop-and-Wait File Transfer Protocol Implementations

## Project Purpose

This was a Computer Networking project where we learned how to build a client, server, and a basic web cache all from scratch. The goal was to implement two different file transfer protocols—**TCP** and **Stop-and-Wait**—and compare their performance when transferring files across a network connection.

Instead of using built-in high-level libraries for sending and receiving files, we implemented our own custom logic for handling packets, timeouts, retransmissions, and acknowledgments (especially for Stop-and-Wait).

---

## High-Level Functionality

### TCP File Transfer

This mode uses standard **TCP sockets** but with my own logic for breaking down and reconstructing files at the application layer. I manually handle the file chunking and reconstruction, rather than relying on just `sendfile()` or other high level method calls.

Workflow for TCP Mode:

1. **Client connects to Server over TCP**
2. **Client requests a file**
3. **Server reads file and sends it in chunks**
4. **Client receives and reconstructs the file locally**

---

### Stop-and-Wait File Transfer

This mode implements a very basic **Stop-and-Wait protocol over UDP** to simulate a reliable connection on top of an unreliable one. We had to build out:

* Packet numbering
* Acknowledgments
* Timeouts
* Retransmissions for lost packets

Workflow for Stop-and-Wait Mode:

1. **Client sends a packet to the Server and waits for an ACK**
2. **If no ACK is received within the timeout window, the client retransmits**
3. **Repeat until the entire file is successfully sent and acknowledged packet-by-packet**

This lets me test how Stop-and-Wait behaves compared to TCP in real file transfers

---

### Web Cache Component

In addition to file transfers, I also coded up a simple **web cache**. The cache stores files that the server has already sent once, so if a client asks for the same file again, the server can deliver it faster without having to re-read it from disk.

---

## Testing & Performance Comparison

Once both protocols were implemented, I measured and compared the **total transfer time** for different file sizes over both TCP and Stop-and-Wait. This helped me see the real-world tradeoffs between reliability and speed.

---

## Running the Project

If you want to run this on your own machine:

1. **Download all the `.java` files** and make sure they sit in the same directory.
2. **Compile everything:**

   ```bash
   javac *.java
   ```
3. **Run the Server:**

   ```bash
   java Server
   ```
4. **Run the Cache Server (also in a separate terminal window or machine):**

   ```bash
   java Cache
   ```
5. **Run the Client (in a separate terminal window or machine):**

   ```bash
   java Client
   ```
   
---

## Please Note:

If you are having a **"package cannot be resolved"** error for either:

* `cache.java`
* `client.java`

Within the directory where it is giving a package problem, you need to compile the code together.

You can do this with the following command:

```bash
javac -d classes <path to the .java file for package you are trying to import> <Java file you are trying to compile>
```

Then to run the code:

```bash
java -cp classes <java file you are trying to run> <command line arguments>
```

This will let Java properly resolve the package structure and run the code without import issues.

---
