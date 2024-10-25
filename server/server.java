package server;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class server {

    private static ServerSocket serverSocket = null;

    public static void main(String args[]) {

        int serverPortNumber = Integer.parseInt(args[0]);

        // Verifies the port range is within range to comply with ITS security
        if ((serverPortNumber < 20000) | (serverPortNumber > 24000)) {
            System.out.println("Error: Can't start server on port not within 20000-24000 range");
            return;
        }

        // Finds what protocol is being used and sets booleans accordingly
        // This is so that we don't have to check the value of a string every time
        // we want to find what protocol is being used.
        boolean usingSNW = false;
        boolean usingTCP = false;
        if (args[1].toLowerCase().equals("tcp")) {
            usingTCP = true;
        } else if (args[1].toLowerCase().equals("snw")) {
            usingSNW = true;
        } else {
            System.out.println("Error: Incorrect protocol entered\nMust be either TCP or SNW");
            return;
        }

        System.out.println("Server Port Number: " + serverPortNumber);
        if (usingTCP == true) {
            System.out.println("\nProtocol: TCP");
        } else {
            System.out.println("\nProtocol: SNW");
        }

        /*
         * USING TCP PROTOCOL WE SET UP A LISTENER TO VERIFY THAT FILE UPLOAD WORKS
         */

        try {

            // Starts up server side listening connection
            serverSocket = new ServerSocket(serverPortNumber);
            System.out.println("Server is started...");
            System.out.println("Waiting for incoming connections...");

            while (true) {

                // Helps manage our "live" connection with the client to send and recieve data
                Socket socket = serverSocket.accept();

                System.out.println("Client accepted");

                // Creates a listening/sending stream from our current running socket connection
                DataInputStream incomingData = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream messageStream = new DataOutputStream(socket.getOutputStream());

                // The first thing sent to us by the client is the file name
                String fileName = incomingData.readUTF();

                //System.out.println("Receiving file: " + fileName);

                // Now we create the file with the proper file name in the designated server_fl
                // directory
                File outputFile = new File("../server_fl", fileName);

                // Read the file size from the client
                long fileSize = incomingData.readLong();


                //System.out.println("File created and placed in directory...");
                //System.out.println("File Location: " + outputFile.getAbsolutePath());
                //System.out.println("File Name: " + outputFile.getName());
                //System.out.println("File size: " + fileSize);

                // Allows us to write to the newly created file
                FileOutputStream outgoingData = new FileOutputStream(outputFile);

                // Reads the incoming data from our stream and writes it to our new file
                long totalBytesRead = 0;
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (totalBytesRead < fileSize && (bytesRead = incomingData.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    outgoingData.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                //System.out.println("File written to, now closing...");
                
                // Needed before continuing to send a message back
                outgoingData.close();
                
                //System.out.println("Sending message back to client...");

                // Sends the success message to the client to alert them
                messageStream.writeUTF("File successfully uploaded");

                messageStream.flush();

                //System.out.println("Message sent");

                // Maintence to close off all out communications
                incomingData.close();
                messageStream.close();
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("Server File Recieve Error: \n" + e.getMessage());
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}