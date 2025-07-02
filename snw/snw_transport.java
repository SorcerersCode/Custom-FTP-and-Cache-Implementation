package snw;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;

public class snw_transport {
    private static final int CHUNK_SIZE = 1000;
    private static final int TIMEOUT = 1000;
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;

    public snw_transport(InetAddress targetAddress, int targetPort) throws SocketException {
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);
    }

    public snw_transport(DatagramSocket udpSocket) throws SocketException {
        this.socket = udpSocket;
    }

    public void upload(File fileToUpload) throws IOException {

        if (!fileToUpload.exists()) {
            System.out.println("Error: The file you are trying to upload does not exist. Please try again...");
            return;
        }

        String fileName = fileToUpload.getName();

        // Grabs the size of the file we are trying to upload
        byte[] fileBytes = Files.readAllBytes(fileToUpload.toPath());
        int fileLength = fileBytes.length;

        // Send the name of the file we are looking to upload
        sendFileName(fileName);

        // So the recieving end knows how long to listen for (byte count wise)
        sendFileLength(fileLength);

        sendFileData(fileLength, fileBytes);

    }

    public void sendFileName(String fileName) throws IOException {

        // Turns name into sentable bytes
        byte[] lengthBytes = fileName.getBytes();
        DatagramPacket lengthPacket = new DatagramPacket(lengthBytes, lengthBytes.length, targetAddress, targetPort);

        // Send the data over
        socket.send(lengthPacket);

        // Timeout handling for length message
        try {
            receiveAck();

            // There had to be an issue so we throw an error and close for maintence
        } catch (SocketTimeoutException e) {
            System.out.println("Did not receive data. Terminating.");

            // Close opened connections for maintence
            socket.close();
        }
    }

    public String receiveFileName() throws IOException {

        try {

            byte[] buffer = new byte[CHUNK_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Receive the packet containing the file name
            socket.receive(packet);

            // Convert the received bytes to a String and trim any excess whitespace
            String fileName = new String(packet.getData(), 0, packet.getLength()).trim();

            // Sends an acknowledgment back to the sender since it needs data to finish ACK
            sendAck(packet.getAddress(), packet.getPort());

            return fileName;

        } catch (SocketTimeoutException e) {
            return null;
        }

    }

    public void sendFileLength(int length) throws IOException {

        // Follows the string format specified in project specifications
        String lengthMsg = "LEN:" + length;

        // Create a new data stream with UDP
        byte[] lengthBytes = lengthMsg.getBytes();
        DatagramPacket lengthPacket = new DatagramPacket(lengthBytes, lengthBytes.length, targetAddress, targetPort);

        // Send the data over
        socket.send(lengthPacket);

        System.out.println("Sent file length: " + lengthPacket);

    }

    public int receiveFileLength() throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);

        // Convert the received bytes to a String and get rid of excess white space
        String lengthMessage = new String(packet.getData(), 0, packet.getLength()).trim();

        // Validate the message format and extract the length
        if (lengthMessage.startsWith("LEN:")) {
            try {
                // Parse the length from the message
                int length = Integer.parseInt(lengthMessage.substring(4).trim());

                // Send an acknowledgment back to the sender
                sendAck(packet.getAddress(), packet.getPort());

                // Start the timer until the first DATA packet is received
                socket.setSoTimeout(TIMEOUT);

                System.out.println("Received file length: " + length);

                return length;

            } catch (NumberFormatException e) {
                System.out.println("Invalid length format received: " + lengthMessage);
                return -1;
            }
        } else {
            System.out.println("Invalid length message format. Expected 'LEN:<length>'.");
            return -1;
        }
    }

    public void sendAck(InetAddress targetAddress, int targetPort) {
        try {
            String ackMessage = "ACK";
            byte[] ackBuffer = ackMessage.getBytes();

            // Create the UDP packet with the ACK message, target IP address, and target
            // port
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, targetAddress, targetPort);

            socket.send(ackPacket);

            // Start the countdown until Ack is received
            socket.setSoTimeout(TIMEOUT);

        } catch (IOException e) {
            System.out.println("Error sending ACK: " + e.getMessage());
        }
    }

    private void receiveAck() throws IOException {
        byte[] ackBuffer = new byte[100];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
        socket.receive(ackPacket);

        // Now we can reset the timer since we received the ACK
        socket.setSoTimeout(0);

        String ackMsg = new String(ackPacket.getData(), 0, ackPacket.getLength());
        if (!ackMsg.equals("ACK")) {
            throw new IOException("Unexpected message received instead of ACK");
        }
    }

    private void sendFin(InetAddress targetAddress, int targetPort) throws IOException {
        String finMsg = "FIN";
        byte[] finBytes = finMsg.getBytes();
        DatagramPacket finPacket = new DatagramPacket(finBytes, finBytes.length,
                targetAddress, targetPort);
        socket.send(finPacket);
    }

    private void receiveFin(InetAddress targetAddress, int targetPort) throws IOException {
        // Upon receiving the FIN message, we need to close the socket connection
        byte[] buffer = new byte[CHUNK_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // This is to absorb any of the other ACK packets that are being sent
        // Garuntees that the last packet we receive before closing the socket is FIN
        boolean finReceived = false;
        while (!finReceived) {
            try {
                socket.receive(packet);
                String receivedMsg = new String(packet.getData(), 0, packet.getLength()).trim();

                if (receivedMsg.equalsIgnoreCase("FIN")) {
                    finReceived = true;
                    socket.setSoTimeout(0);
                    socket.close();
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout waiting for FIN. Terminating.");
            }
        }

    }

    public void sendFileData(int fileLength, byte[] fileBytes) throws IOException {
        // Sends the bytes in chunks until all are transmitted
        for (int bytesLeftOffOn = 0; bytesLeftOffOn < fileLength; bytesLeftOffOn += CHUNK_SIZE) {
            int remainingBytes = Math.min(CHUNK_SIZE, fileLength - bytesLeftOffOn);

            byte[] byteChunk = Arrays.copyOfRange(fileBytes, bytesLeftOffOn, bytesLeftOffOn + remainingBytes);

            sendFileChunk(byteChunk);
        }

        receiveFin(targetAddress, targetPort);
    }

    private void sendFileChunk(byte[] byteChunk) throws IOException {

        DatagramPacket dataPacket = new DatagramPacket(byteChunk, byteChunk.length, targetAddress, targetPort);

        socket.send(dataPacket);

        // Make sure we receive the ACK before continuing
        try {
            receiveAck();

            // Error not getting the ACK symbol so we catch and close open ports for
            // maintence
        } catch (SocketTimeoutException e) {
            System.out.println("Did not receive ACK. Terminating.");

            // For maintence
            socket.close();
        }
    }

    /*
     * Implementation for recieving a file via SNW
     */
    public boolean receiveFileData(File receivedFile, long expectedLength) {

        byte[] buffer = new byte[CHUNK_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        long totalBytesExpected;
        long totalBytesReceived = 0;
        boolean receiving = true;

        // Create the file output stream so we can write to it
        try (FileOutputStream fileStream = new FileOutputStream(receivedFile)) {

            // Set totalBytesExpected from the expectedLength parameter
            totalBytesExpected = expectedLength;

            // Receive the actual content of the file in chunks
            while (receiving) {
                try {
                    socket.receive(packet);

                    int bytesReceived = packet.getLength();
                    totalBytesReceived += bytesReceived;

                    // Write the received chunk to file
                    fileStream.write(packet.getData(), 0, bytesReceived);

                    // Send ACK for each chunk
                    sendAck(packet.getAddress(), packet.getPort());

                    // Check if all expected bytes have been received
                    if (totalBytesReceived >= totalBytesExpected) {
                        receiving = false;
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("Did not receive data. Terminating.");
                    break;
                }
            }

            // Send FIN message back to client to let them know we got it all
            sendFin(packet.getAddress(), packet.getPort());
            return true;

        } catch (IOException e) {
            System.out.println("File receive error: " + e.getMessage());
        }

        return false;
    }

}
