
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

public class cache {

    private static ServerSocket cacheSocket = null;

    public static void main(String args[]) {

        // Used to resolve the IP addresses to the proper type and catch any
        // initialization errors
        try {
            InetAddress serverIpAddress = InetAddress.getByName(args[1]);
        } catch (UnknownHostException e) {
            System.out.println("Error Resolving IP Addresses on startup:\n" + e);
        }

        // Initializes the basic info to continue the communications
        int serverPortNumber = Integer.parseInt(args[2]);
        int cachePortNumber = Integer.parseInt(args[0]);

        // Verifies the port range is within range to comply with ITS security
        if ((serverPortNumber < 20000) | (serverPortNumber > 24000)) {
            System.out.println("Error: Server port not within 20000-24000 range");
            return;
        }

        // Finds what protocol is being used and sets booleans accordingly
        // This is so that we don't have to check the value of a string every time
        // we want to find what protocol is being used.
        boolean usingSNW = false;
        boolean usingTCP = false;
        if (args[3].toLowerCase().equals("tcp")) {
            usingTCP = true;
        } else if (args[3].toLowerCase().equals("snw")) {
            usingSNW = true;
        } else {
            System.out.println("Error: Incorrect protocol entered\nMust be either TCP or SNW");
            return;
        }

        /*
         * USING TCP PROTOCOL WE SET UP A LISTENER TO VERIFY THAT FILE UPLOAD WORKS
         */

        try {

            // Starts up cache socket
            cacheSocket = new ServerSocket(serverPortNumber);
            System.out.println("Chache is started...");
            System.out.println("Waiting for incoming connections...");

            while (true) {
                // Cache recieves request
                Socket socket = cacheSocket.accept();

                System.out.println("Client accepted");

                // Creates a listening/sending stream from our current running socket connection
                DataInputStream incomingData = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream messageStream = new DataOutputStream(socket.getOutputStream());

                // Check's it's directory for the file

                // If file is found start uploading it to client with TCP put

                // Once file done uploading, cache sends and prints "File delivered from cache."

                // Close connection on cache for file upload

                // Close connection on client for recieving

                // If file is not found on cache server, create tcp_transport instance to
                // request server

                // tcp_transport.get(file) and wait for response

                // Recieve file name, create it in cache location

                // Recieve file binary, write to the newly created file

                // Close connection between cache and server

                // Once done, serve file to client with message "File delivered from server."

                // Close connection between client and cache

            }
        } catch (IOException e) {
            System.out.println("Server File Recieve Error: \n" + e.getMessage());
        }
    }
}

// System.out.println("Server IP Address: " + args[1]);
// System.out.println("Server Port Number: " + serverPortNumber);
// System.out.println("Cache Port Running on: " + cachePortNumber);
// if (usingTCP == true) {
// System.out.println("\nProtocol: TCP");
// } else {
// System.out.println("\nProtocol: SNW");
// }