package client;

import java.io.*;
import java.net.Socket;

public class ClientFileSender implements Runnable {
    private final String filePath;
    private final String uuid;
    private final String host;
    private final int port;

    public ClientFileSender(String filePath, String uuid, String host, int port) {
        this.filePath = filePath;
        this.uuid = uuid;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(host, port);
            InputStream inputStream = new FileInputStream(filePath);
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(uuid.getBytes());
            outputStream.write('S');
            inputStream.transferTo(outputStream);
            socket.close();

            System.out.println("File is uploaded.");

        } catch (IOException e) {
            System.out.println("An error occurred during file transfer: " + e.getMessage());
        }
    }
}
