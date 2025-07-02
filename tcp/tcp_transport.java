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

    /*
     * Upload: Sends a file from the client instance to the server
     */
    public void upload(File file) {

        // Prevents the console from crashing if the file doesn't exist
        if (!file.exists()) {
            System.out.println("Error: The file you are trying to upload does not exist. Please try again...");
            return;
        }

        try {

            // Used to send the file stream contents over connection
            FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

            // Created streams for in bound and out bound traffic (as well as read/write)
            DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());
            DataInputStream incomingData = new DataInputStream(socket.getInputStream());

            // Server first expects the file name
            sendFileName(file, outgoingData);

            // Server needs to know what operation we are doing with the given file
            sendCommand("upload", outgoingData);

            // Sends the file size so the receiving end knows when to quit reading
            long fileSize = sendFileLength(file, outgoingData);

            // Send the file and its contents
            sendFileData(file, fileSize, outgoingData, fileInputStream);

            // Need to provide the parameters so that they can be closed
            receiveUploadMessageFromServer(incomingData, outgoingData, fileInputStream);

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

    public File download(File file) {
        try {
            // Get name of file that we are requesting
            String fileName = file.getName();

            // Set up a DataOutputStream
            DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());

            // Send the name and command being requested to target
            sendFileName(file, outgoingData);
            sendCommand("download", outgoingData);

            // Set up an DataInputStream
            DataInputStream incomingData = new DataInputStream(socket.getInputStream());

            // The size of the file that we request will be returned to us (or -1)
            long fileSize = receiveFileLength(incomingData);

            // If the size of the file (aka response from request) = -1
            // Print out that message saying the file doesn't exist
            // on neither cache nor server
            if (fileSize == -1) {
                // Close streams for cleanup
                incomingData.close();
                outgoingData.close();
                return null;
            }

            // Create a new file with the name of the file we are requesting
            // Since we didn't get -1 we know the file exists, so we create an empty one
            // here first
            File requestedFile = new File(fileName);

            // Allows us to write to the newly created file
            FileOutputStream outputData = new FileOutputStream(requestedFile);

            receiveFileData(requestedFile, incomingData, outputData, fileSize);

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

    private void sendFileName(File file, DataOutputStream outgoingData) throws IOException {
        outgoingData.writeUTF(file.getName());
    }

    private long sendFileLength(File file, DataOutputStream outgoingData) throws IOException {
        long fileSize = file.length();
        outgoingData.writeLong(fileSize);

        return fileSize;
    }

    private long receiveFileLength(DataInputStream incomingData) throws IOException {
        return Long.parseLong(incomingData.readUTF());
    }

    private void sendCommand(String command, DataOutputStream outgoingData) throws IOException {
        outgoingData.writeUTF(command);
    }

    private void sendFileData(File file, long fileSize, DataOutputStream outgoingData, FileInputStream fileInputStream)
            throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        long totalBytesSent = 0;
        while (totalBytesSent < fileSize && (bytesRead = fileInputStream.read(buffer)) != -1) {
            outgoingData.write(buffer, 0, bytesRead);
            totalBytesSent += bytesRead;
        }

        // Tells the server that all the data has been sent over
        outgoingData.flush();
    }

    private String receiveFileData(File requestedFile, DataInputStream incomingData, FileOutputStream outputData,
            long fileSize) throws IOException {
        // Create while look that writes the DataInputStream to the fileInputStream
        // Add a counter of bytes writted and add every time
        // Once it matches or exceeds the saved file size
        long totalBytesRead = 0;
        byte[] buffer = new byte[4096];
        int bytesRead;
        while (totalBytesRead < fileSize && (bytesRead = incomingData.read(buffer, 0,
                (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
            outputData.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }

        // Now that we have the file we want to print out where it came from
        String sourceMessage = incomingData.readUTF();
        System.out.println(sourceMessage);

        return sourceMessage;
    }

    private void receiveUploadMessageFromServer(DataInputStream incomingData, DataOutputStream outgoingData,
            FileInputStream fileInputStream) throws IOException {
        /*
         * This next code segment is to listen and wait for the server to
         * tell us that the file has been uploaded successfully
         */
        String serverMessage = incomingData.readUTF();
        System.out.println(serverMessage);

        // Since we got the message from the server, we can close the streams
        fileInputStream.close();
        outgoingData.close();
        incomingData.close();
    }
}
