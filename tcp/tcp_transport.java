package tcp;

import java.net.InetAddress;
import java.net.Socket;
import java.net.*;
import java.io.*;

public class tcp_transport {

    private InetAddress targetIPAddress;
    private int targetPortNumber;
    private Socket socket;

    public tcp_transport(InetAddress ipAddress, int portNumber) {
        this.targetIPAddress = ipAddress;
        this.targetPortNumber = portNumber;

        try {
            socket = new Socket(targetIPAddress, targetPortNumber);
        } catch (UnknownHostException exception) {
            System.out.println(exception.getMessage());
            return;
        } catch (IOException i) {
            System.out.println(i.getMessage());
            return;
        }
    }

    public void upload(File file) {

        try {

            // The stream used to upload the file itself and its contents
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
            // The stream used to upload the name of the file
            DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());
            // Stream used to
            DataInputStream incomingData = new DataInputStream(socket.getInputStream());

            // Sends the filename to the server
            outgoingData.writeUTF(file.getName());

            // Sends the file size to know when to quit reading
            long fileSize = file.length();
            outgoingData.writeLong(fileSize);

            // Send the file and its contents
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesSent = 0;
            while (totalBytesSent < fileSize && (bytesRead = fileInputStream.read(buffer)) != -1) {
                outgoingData.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;
            }

            /*
             * This next code segment is to listen and wait for the server to
             * tell us that the file has been uploaded successfully
             */
            // Tells the server that all the data has been sent over
            outgoingData.flush();

            //System.out.println("File uploaded\nNow waiting for message from server");

            String serverMessage = incomingData.readUTF();
            System.out.println(serverMessage);

            fileInputStream.close();
            outgoingData.close();
            incomingData.close();

        } catch (IOException e) {
            System.out.println("Error during file upload: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public File get(File file) {
        try {

            // Get name of file that we are requesting
            String fileName = file.getName();

            // Set up a DataOutputStream
            DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());

            // Send the name of the file being requested to target
            outgoingData.writeUTF(fileName);

            // Set up an DataInputStream
            DataInputStream incomingData = new DataInputStream(socket.getInputStream());

            // The size of the file that we request will be returned to us (or -1)
            long fileSize = Long.parseLong(incomingData.readUTF());

            // If the size of the file (aka response from request) = -1
                // Print out that message saying the file doesn't exist
                // on neither cache nor server
            if(fileSize == -1){
                // Close streams for cleanup
                incomingData.close();
                outgoingData.close();
                return null;
            }

            // Create a new file with the name of the file we are requesting
            // Since we didn't get -1 we know the file exists, so we create an empty one here first
            File requestedFile = new File(fileName);

            
            // Allows us to write to the newly created file
            FileOutputStream outputData = new FileOutputStream(requestedFile);

            // Create while look that writes the DataInputStream to the fileInputStream
                // Add a counter of bytes writted and add every time
                // Once it matches or exceeds the saved file size, break and close stream
            
            // Reads the incoming data from our stream and writes it to our new file
            long totalBytesRead = 0;
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (totalBytesRead < fileSize && (bytesRead = incomingData.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                outgoingData.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            // Closing all the data streams for cleanup
            outgoingData.close();
            incomingData.close();
            outputData.close();
            
            // Return the new file
            return requestedFile;

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Operation was unsucessfull so we return null for application to deal
        return null;
    }

}
