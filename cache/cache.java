
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import snw.snw_transport;
import tcp.tcp_transport;

public class cache {

    private static ServerSocket cacheSocket = null;
    private static final String CACHE_DIRECTORY = "../cache_fl";

    public static void main(String args[]) throws IOException {

        InetAddress serverIpAddress = null;

        // Used to resolve the IP addresses to the proper type and catch any
        // initialization errors
        try {
            serverIpAddress = InetAddress.getByName(args[1]);
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

        if (usingTCP) {

            // Starts up cache socket
            cacheSocket = new ServerSocket(cachePortNumber);
            try {
                // Infinite while loop allows for cache to act like a server
                // Accepts each incoming socket connection and treats them accordingly
                while (true) {
                    // Cache recieves request
                    Socket socket = cacheSocket.accept();

                    // Needed to send data to and from the client we are interacting with
                    DataInputStream incomingClientData = new DataInputStream(
                            new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream outgoingClientData = new DataOutputStream(socket.getOutputStream());

                    // Get the name of the file being requested
                    String fileName = incomingClientData.readUTF();

                    // Recieve type of command (we don't do anything with it sice the cache is not
                    // the server)
                    String command = incomingClientData.readUTF();

                    // If we set the data stored at the memory address to null, java's garbage
                    // collector will release from memory
                    command = null;

                    // Will allow us to check to see if the file exists within our directory
                    File requestedFile = Paths.get(CACHE_DIRECTORY, fileName).toFile();

                    /*
                     * FILE ALREADY EXISTS ON CACHE
                     * 
                     * This code segment takes care of file download to client if it
                     * already exists within the cache's storage
                     */
                    if (requestedFile.exists()) {

                        // If file is found get the file size in string format
                        outgoingClientData.writeUTF(String.valueOf(requestedFile.length()));

                        // Open the file and stream it's contents over connection
                        try (FileInputStream fileStream = new FileInputStream(requestedFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesSent = 0;
                            while (totalBytesSent < requestedFile.length()
                                    && (bytesRead = fileStream.read(buffer)) != -1) {
                                outgoingClientData.write(buffer, 0, bytesRead);
                                totalBytesSent += bytesRead;
                            }
                        }

                        // Send a message letting client know we sent file from cache
                        outgoingClientData.writeUTF("File delivered from cache.");
                        outgoingClientData.flush();

                        /*
                         * FILE DOES NOT EXIST ON CACHE
                         * 
                         * Requests the file to the server to see if it has it
                         * If the server has it then it will save it and send it to the client
                         */
                    } else {

                        // If file is not found on cache server, create tcp_transport instance to
                        // request server
                        tcp_transport serverConnection = new tcp_transport(serverIpAddress, serverPortNumber);

                        // Download the file from the server
                        File fileReturned = serverConnection.download(requestedFile);

                        // Prevents the cache from crashing if the file doesn't exist on either server
                        // nor cache
                        if (fileReturned != null) {
                            // Recieve the file, put it in cache location for later use
                            MoveFileToDirectory(fileReturned, CACHE_DIRECTORY);

                            // Send the file back to the client
                            // Client is waiting for the file size, so we calulate it and send it
                            sendFileLength(outgoingClientData, requestedFile);

                            // Open the file so we can stream it's contents over connection
                            FileInputStream fileStream = new FileInputStream(requestedFile);

                            // Send over the contents of the file
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long totalBytesSent = 0;
                            while (totalBytesSent < requestedFile.length()
                                    && (bytesRead = fileStream.read(buffer)) != -1) {
                                outgoingClientData.write(buffer, 0, bytesRead);
                                totalBytesSent += bytesRead;
                            }

                            // Tells the recieving end that all data was transmitted
                            outgoingClientData.flush();

                            // Send it to client now that it has been processed
                            outgoingClientData.writeUTF("File delivered from server.");

                            // Close all streams for maintence
                            incomingClientData.close();
                            outgoingClientData.close();
                            fileStream.close();
                        } else {
                            // We send back -1 as file size as per convention
                            outgoingClientData.writeUTF("-1");
                            System.out.println("Error: File requested that doesn't exist on server nor cache");
                            incomingClientData.close();
                            outgoingClientData.close();
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Server File Recieve Error: \n" + e.getMessage());
            }

        } else if (usingSNW) {

            DatagramSocket udpSocket = new DatagramSocket(cachePortNumber);

            // Initialize the transport receiver
            snw_transport snwCacheReceiver = new snw_transport(udpSocket);
            try {
                while (true) {
                    // To prevent the udp from timing out while trying to listen
                    udpSocket.setSoTimeout(0);

                    // Receiving file name initiates the udp file process
                    String fileName = snwCacheReceiver.receiveFileName();

                    if (fileName == null) {
                        System.out.println("Failed to receive file name. Terminating.");
                        continue;
                    }

                    // Will allow us to check to see if the file exists within our directory
                    File requestedFile = Paths.get(CACHE_DIRECTORY, fileName).toFile();

                    /*
                     * FILE ALREADY EXISTS ON CACHE
                     * 
                     * This code segment takes care of file download to client if it
                     * already exists within the cache's storage
                     */
                    if (requestedFile.exists()) {

                        System.out.println("File found on cache...");

                        System.out.println("Calculating file size...");

                        // Calculate size of file we will be sending
                        byte[] fileBytes = Files.readAllBytes(requestedFile.toPath());
                        int fileLength = fileBytes.length;

                        System.out.println("Sending over file length...");

                        // Send size of file to receiving end
                        snwCacheReceiver.sendFileLength(fileLength);

                        System.out.println("Sending file over now...");

                        // Send the data over now
                        snwCacheReceiver.sendFileData(fileLength, fileBytes);

                        /*
                         * FILE DOES NOT EXIST ON CACHE
                         * 
                         * Requests the file to the server to see if it has it
                         * If the server has it then it will save it and send it to the client
                         */
                    } else {

                        // If file is not found on cache server, create tcp_transport instance to
                        // request server
                        System.out.println("Cache doesn't have file, creating connection to server...");

                        // snw_transport get file from server

                        System.out.println("File returned from server: " + requestedFile);
                    }
                }
            } catch (IOException e) {
                System.out.println("Server File Recieve Error: \n" + e.getMessage());
            }

        }

    }

    private static void MoveFileToDirectory(File fileToMove, String targetDirectory) {

        File targetDir = new File(targetDirectory);

        // Create redundancy for file directory
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Define the path to move the file to within the target directory
        Path targetPath = new File(targetDir, fileToMove.getName()).toPath();

        try {
            // Moves the file to the directory (overwrites if it already exists)
            Files.move(fileToMove.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // System.out.println("File moved to directory: " + targetPath);
        } catch (IOException e) {
            System.out.println("Error moving file: " + e.getMessage());
        }
    }

    private static void sendFileLength(DataOutputStream outgoingData, File file) throws IOException {
        outgoingData.writeUTF(String.valueOf(file.length()));
    }
}