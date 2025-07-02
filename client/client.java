package client;

import tcp.tcp_transport;
import snw.snw_transport;
import client.client;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class client {

    public static void main(String args[]) throws IOException {

        // Resolves the IP addresses and catch any initialization errors
        InetAddress serverIpAddress = null;
        InetAddress cacheIpAddress = null;
        try {
            serverIpAddress = InetAddress.getByName(args[0]);
            cacheIpAddress = InetAddress.getByName(args[2]);
        } catch (UnknownHostException e) {
            System.out.println("Error Resolving IP Addresses on startup:\n" + e);
        }

        // Initializes the basic info to continue the communications
        int serverPortNumber = Integer.parseInt(args[1]);
        int cachePortNumber = Integer.parseInt(args[3]);

        // Verifies the port range is within range to comply with ITS security
        if ((serverPortNumber < 20000) || (serverPortNumber > 24000)) {
            System.out.println("Error: Server port not within 20000-24000 range");
            return;
        }

        // Finds what protocol is being used and sets booleans accordingly
        // This is so that we don't have to check the value of a string every time
        // we want to find what protocol is being used.
        boolean usingSNW = false;
        boolean usingTCP = false;
        if (args[4].toLowerCase().equals("tcp")) {
            usingTCP = true;
        } else if (args[4].toLowerCase().equals("snw")) {
            usingSNW = true;
        } else {
            System.out.println("Error: Incorrect protocol entered\nMust be either TCP or SNW");
            return;
        }

        // Sets up the variables that will be used each input cycle
        Scanner keyboard = new Scanner(System.in);
        String userInput = null;
        String userCommand = null;
        String userFile = null;

        do {
            System.out.print("Enter Command: ");
            userInput = keyboard.nextLine();

            // Parse user input for the command and file path
            String[] userInputParts = userInput.split(" ", 2);
            userCommand = userInputParts[0];

            // Used in case the user inputs only one word/command
            // Prevents IndexOutOfBounds error for quit command
            if (userInputParts.length == 2) {
                userFile = userInputParts[1];
            }

            if (userCommand.equalsIgnoreCase("put")) {

                // Regardless of protocol we are using, we will be uploading a file
                // This sets that variable up for that accordingly
                File file = new File(userFile);

                if (usingTCP) {
                    // File uploads only go from client directly to server
                    // That's why we use only the server as the target port and IP
                    tcp_transport serverFileTransport = new tcp_transport(serverIpAddress, serverPortNumber);

                    serverFileTransport.upload(file);

                } else if (usingSNW) {

                    // File uploads only go from client directly to server
                    // That's why we use only the server as the target port and IP
                    snw_transport serverFileTransport = new snw_transport(serverIpAddress, serverPortNumber);

                    serverFileTransport.upload(file);

                    // The FIN message was already sent so it would be messy to send more data after
                    // Instead we print out to console if it reaches this line
                    System.out.println("File successfully uploaded.");
                }

                // Handles file downloads for both TCP and SNW instances
            } else if (userCommand.equalsIgnoreCase("get")) {

                // Regardless of protocol we are using, we will be uploading a file
                // This sets that variable up for that accordingly
                File requestedFile = new File(userFile);

                if (usingTCP) {
                    // Request file from Cache
                    tcp_transport cacheFileRequest = new tcp_transport(cacheIpAddress, cachePortNumber);

                    // Get the file from the cache/server
                    File fileReturned = cacheFileRequest.download(requestedFile);

                    if (fileReturned != null) {
                        MoveFileToDirectory(fileReturned, "../client_fl");
                    } else {
                        System.out.println("Error: File does not exist on either cache nor server");
                    }
                } else if (usingSNW) {

                    System.out.println("Now entered the SNW protocol...");

                    // Create snw_transport instance
                    snw_transport cacheFileRequest = new snw_transport(cacheIpAddress, cachePortNumber);

                    System.out.println("snw isntance made...");

                    // Send over name of file
                    cacheFileRequest.sendFileName(requestedFile.getName());

                    System.out.println("File name sent over...");

                    // Set up a listener mechanism that waits for length
                    long fileLength = cacheFileRequest.receiveFileLength();

                    System.out.println("File length received...");

                    // Once you get the length, wait/receive the data
                    cacheFileRequest.receiveFileData(requestedFile, fileLength);

                    System.out.println("File received from cache...");

                }

            } else {
                if (!userCommand.toLowerCase().equals("quit")) {
                    System.out.println("Error: Invalid command or format\nShould be <command> <filePath>");
                }

            }

        } while (!userCommand.equalsIgnoreCase("quit"));

        keyboard.close();
        System.out.println("Exiting Program!");
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

}