package server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;

import snw.snw_transport;

public class server {

    private static final String SERVER_DIRECTORY = "../server_fl";
    private static ServerSocket serverSocket = null;

    public static void main(String args[]) throws InterruptedException {

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
        if (args[1].equalsIgnoreCase("tcp")) {
            usingTCP = true;
        } else if (args[1].equalsIgnoreCase("snw")) {
            usingSNW = true;
        } else {
            System.out.println("Error: Incorrect protocol entered\nMust be either TCP or SNW");
            return;
        }

        /*
         * USING TCP PROTOCOL WE SET UP A LISTENER TO VERIFY THAT FILE UPLOAD WORKS
         */

        try {
            if (usingTCP) {
                // Starts up server side listening connection
                serverSocket = new ServerSocket(serverPortNumber);

                while (true) {

                    // Helps manage our "live" connection with the client to send and recieve data
                    Socket socket = serverSocket.accept();

                    // Creates a listening/sending stream from our current running socket connection
                    DataInputStream incomingData = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());

                    // The first thing sent to us by the client is the file name
                    String fileName = incomingData.readUTF();

                    // Clears out any previous command operations that were performed
                    String command = null;
                    // Next is the operation we will be performing (either upload or download a
                    // file)
                    command = incomingData.readUTF();

                    // Creates file and file path so we can check if it exists already
                    File requestedFile = Paths.get(SERVER_DIRECTORY, fileName).toFile();

                    if (command.equalsIgnoreCase("upload")) {
                        // Read the file size from the client
                        long fileSize = incomingData.readLong();

                        try (FileOutputStream fileStream = new FileOutputStream(requestedFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesRead = 0;
                            while (totalBytesRead < fileSize && (bytesRead = incomingData.read(buffer)) != -1) {
                                fileStream.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                            }
                        }

                        // Sends the success message to the client
                        outgoingData.writeUTF("File successfully uploaded.");

                        // Command 'get' indicates file download, check to see if we have the file
                    } else if (command.equalsIgnoreCase("download")) {
                        if (requestedFile.exists()) {
                            // Commence with the file download proccess to the cache following tcp_transport
                            // Client is waiting for the file size, so we calulate it and send it
                            outgoingData.writeUTF(String.valueOf(requestedFile.length()));

                            // Open the file so we can stream it's contents over connection
                            FileInputStream fileStream = new FileInputStream(requestedFile);

                            // Send over the contents of the file
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesSent = 0;
                            while (totalBytesSent < requestedFile.length()
                                    && (bytesRead = fileStream.read(buffer)) != -1) {
                                outgoingData.write(buffer, 0, bytesRead);
                                totalBytesSent += bytesRead;
                            }

                            // Tells the recieving end that all data was transmitted
                            outgoingData.flush();

                            // Send a message letting client know we sent file from cache
                            outgoingData.writeUTF("File delivered from server.");

                            // To make sure the file message is received by client before closing connection
                            Thread.sleep(100);

                            // Close all streams for maintence
                            fileStream.close();
                            incomingData.close();
                            outgoingData.close();

                        } else {
                            // -1 Indicates to tcp instance that file does not exist
                            outgoingData.writeUTF("-1");
                        }
                    }

                    // Maintence to close off all out communications
                    socket.close();
                }
            } else if (usingSNW) {
                DatagramSocket udpSocket = new DatagramSocket(serverPortNumber);

                // Initialize the transport receiver
                snw_transport snwReceiver = new snw_transport(udpSocket);

                while (true) {

                    // To prevent the udp from timing out while trying to listen
                    udpSocket.setSoTimeout(0);

                    // Receiving file name initiates the file upload
                    String fileName = snwReceiver.receiveFileName();

                    if (fileName == null) {
                        System.out.println("Failed to receive file name. Terminating.");
                        continue;
                    }

                    // Receive the file length
                    long expectedLength = snwReceiver.receiveFileLength();
                    if (expectedLength < 0) {
                        System.out.println("Failed to receive file length. Terminating.");
                        continue;
                    }

                    // Receive the file data
                    File receivedFile = new File(SERVER_DIRECTORY, fileName);
                    if (!snwReceiver.receiveFileData(receivedFile, expectedLength)) {
                        System.out.println("Failed to receive the file data properly.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Server File Recieve Error: " + e.getMessage());
            System.out.println("More Detail: ");
            e.printStackTrace();
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