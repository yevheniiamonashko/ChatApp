package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileTransferServer implements Runnable {
    private final int port;
    private ServerSocket fileTransferSocket;
    private boolean running = true;

    private final HashMap<String, FileTransferSession> fileTransfers = new HashMap<>();


    public FileTransferServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            fileTransferSocket = new ServerSocket(port);
            System.out.println("File server on port " + port);
            while (running) {
                Socket fileTransferClient = fileTransferSocket.accept();
                new Thread(() -> this.handleConnection(fileTransferClient)).start();
            }


        } catch (IOException e) {
            if (running) {
                System.out.println("File transfer failed to accept the connection: " + e);
            }
        }
    }

    public ServerSocket getFileTransferSocket() {
        return fileTransferSocket;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private void handleConnection(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            byte[] uuidBytes = inputStream.readNBytes(36);
            byte role = inputStream.readNBytes(1)[0];

            String uuid = new String(uuidBytes);


            if (!fileTransfers.containsKey(uuid)) {
                fileTransfers.put(uuid, new FileTransferSession());
            }

            if (role == 'S') {
                fileTransfers.get(uuid).setSender(inputStream);
            } else if (role == 'R') {
                fileTransfers.get(uuid).setReceiver(outputStream);
            } else {
                System.out.println("Invalid role: " + role);
                return;
            }


            System.out.println(fileTransfers.get(uuid).getSender());
            System.out.println(fileTransfers.get(uuid).getReceiver());

            if (fileTransfers.get(uuid).getSender() == null || fileTransfers.get(uuid).getReceiver() == null) {
                System.out.println("Waiting for other party to join.");
                return;
            }


            fileTransfers.get(uuid).getSender().transferTo(fileTransfers.get(uuid).getReceiver());


            fileTransfers.get(uuid).getSender().close();
            fileTransfers.get(uuid).getReceiver().close();
            fileTransfers.remove(uuid);


        } catch (IOException e) {
            System.err.println("Error occurred during establishing connection:"  + e.getMessage());
        }
    }
}
