package client;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class ClientFileReceiver implements Runnable {
    private final String uuid;
    private final String host;
    private final int port;
    private final String checksum;
    private final String fileName;


    public ClientFileReceiver(String checksum, String uuid, String fileName, String host, int port) {
        this.uuid = uuid;
        this.host = host;
        this.port = port;
        this.fileName = fileName;
        this.checksum = checksum;


    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(host, port);

            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(uuid.getBytes());
            outputStream.write('R');

            String downloadDirectory = "downloadedFiles";
            String fullPath = downloadDirectory + File.separator + fileName;

            InputStream inputStream = socket.getInputStream();
            OutputStream fileOutputStream = new FileOutputStream(fullPath);

            inputStream.transferTo(fileOutputStream);

            validateChecksum(fullPath);
            socket.close();




        } catch (IOException e) {
            System.out.println("An error occurred during file transfer: " + e.getMessage());
        }

    }

    public void validateChecksum(String receivedFilePath) {
        try {

            String generatedChecksum = Client.calculateSHA256Hash(receivedFilePath);


            if (generatedChecksum.equals(checksum)) {
                System.out.println("Checksum validation of received file is successful. The file downloaded successfully and stored in downloadedFiles directory.");

            } else {
                System.out.println("Checksum validation failed. The file might be corrupted during the transmission.");
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Error while validating checksum: " + e.getMessage());
        }
    }


}
