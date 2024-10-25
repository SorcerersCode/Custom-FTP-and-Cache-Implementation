package client;
import tcp.tcp_transport;
import client.client;
import server.server;
import java.io.File;
import java.net.*;
import java.util.Scanner;

public class client {

    public static void main(String args[]) {

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

        Scanner keyboard = new Scanner(System.in);
        String userInput = null;
        String userCommand = null;
        String userFile = null;
        do{
            System.out.print("Enter Command: ");
            userInput = keyboard.nextLine();

            // Parse user input for the command and file path
            String[] userInputParts = userInput.split(" ", 2);

            // Assign the variables
            userCommand = userInputParts[0];
            
            // Used in case the user inputs only one word/command
            if(userInputParts.length == 2){
                userFile = userInputParts[1];
            }

            if(userCommand.toLowerCase().equals("put")){

                File file = new File(userFile);

                //System.out.println("File Absolute Path: " + file.getAbsolutePath())
                //System.out.println("Now creating tcp_transport instance...");
                
                // Create the connection needed to upload file to server
                tcp_transport serverFileTransport = new tcp_transport(serverIpAddress, serverPortNumber);

                //System.out.println("tcp_transport instance made");
                //System.out.println("Now attempting to upload file with method");

                serverFileTransport.upload(file);

            } else if (userCommand.toLowerCase().equals("get")){

                File requestedFile = new File(userFile);
                
                // Request file from Cache first
                tcp_transport cacheFileRequest = new tcp_transport(cacheIpAddress, cachePortNumber);

                // Get the file from the cache/server
                File fileReturned = cacheFileRequest.get(requestedFile);

                if(fileReturned == null){
                    System.out.println("Error: File not found on either cache nor server");
                    return;
                }

            } else {
                if(!userCommand.toLowerCase().equals("quit")){
                    System.out.println("Error: Invalid command or format\nShould be <command> <filePath>");
                }
                
            }

        } while(!userCommand.equals("quit"));

        keyboard.close();
        System.out.println("Exiting Program!");
    }
}

// System.out.println("Server IP Address: " + args[0]);
// System.out.println("Server Port Number: " + serverPortNumber);
// System.out.println("\nCache IP Address: " + args[2]);
// System.out.println("Cache Port Number: " + cachePortNumber);
// if(usingTCP == true){
// System.out.println("\nProtocol: TCP");
// } else {
// System.out.println("\nProtocol: SNW");
// }