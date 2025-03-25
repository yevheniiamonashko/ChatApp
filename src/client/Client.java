package client;

import client.handlers.*;
import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import shared.messages.*;
import shared.utility.Command;
import shared.utility.GameMoves;
import shared.utility.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public class Client {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedReader reader; // Client input from console
    private BufferedReader serverReader; // Messages received from server
    private PrintWriter serverWriter; // Messages sent to server
    private final static String HOST = "127.0.0.1";
    private final static int PORT = 1337;
    private final static int FILE_TRANSFER_PORT = 1338;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageHandler messageHandler = new MessageHandler();
    private boolean running = true;
    private boolean isLoggedIn = false;

    private final Map<Command, Consumer<String>> commands = new HashMap<>();
    private String checksum = "";
    private String fileName = "";
    private String filePath = "";


    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public Client() {
        try {
            socket = new Socket(HOST, PORT);

            //input stream for receiving messages
            inputStream = socket.getInputStream();
            //output stream for sending messages
            outputStream = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(System.in));
            serverReader = new BufferedReader(new InputStreamReader(inputStream));
            serverWriter = new PrintWriter(outputStream, true);

            putCommandsForMessagesFromServerIntoMap();


        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void putCommandsForMessagesFromServerIntoMap() {
        this.commands.put(Command.PING, new PingHandler(serverWriter));
        this.commands.put(Command.BROADCAST_RESP, new BroadcastRespHandler(mapper));
        this.commands.put(Command.JOINED, new JoinedHandler(mapper));
        this.commands.put(Command.LEFT, new LeftHandler(mapper));
        this.commands.put(Command.UNKNOWN_COMMAND, new UnknownCommandHandler());
        this.commands.put(Command.BYE_RESP, new ByeRespHandler(mapper, this::close));
        this.commands.put(Command.READY, new ReadyMesHandler(mapper, this::close));
        this.commands.put(Command.ENTER_RESP, new EnterRespHandler(mapper));
        this.commands.put(Command.HANGUP, new HangupHandler(mapper, this::close));
        this.commands.put(Command.PONG_ERROR, new PongErrorHandler(mapper));
        this.commands.put(Command.BROADCAST, new BroadcastHandler(mapper));
        this.commands.put(Command.PARSE_ERROR, new ParseErrorHandler());
        this.commands.put(Command.USER_LIST_RESP, new UserListRespHandler(mapper));
        this.commands.put(Command.PRIVATE_MSG, new PrivateMessageHandler(mapper));
        this.commands.put(Command.PRIVATE_MSG_RESP, new PrivateMsgRespHandler(mapper));
        this.commands.put(Command.GAME_START_RESP, new GameStartRespHandler(mapper));
        this.commands.put(Command.GAME_INVITATION, new GameInviteHandler(mapper));
        this.commands.put(Command.GAME_NOTIFICATION, new GameNotificationHandler(mapper));
        this.commands.put(Command.GAME_MOVE_RESP, new GameMoveRespHandler(mapper));
        this.commands.put(Command.GAME_RESULT, new GameResultHandler(mapper));
        this.commands.put(Command.GAME_CANCELLED, new GameCancelledHandler(mapper));
        this.commands.put(Command.FILE_TRANSFER_REQ, this::receiveFileTransferRequest);
        this.commands.put(Command.FILE_TRANSFER_RESP, new FileTransferRespHandler(mapper, this));
        this.commands.put(Command.FILE_TRANSFER_ACCEPT_RESP, new FileTransferAcceptRespHandler(mapper));
        this.commands.put(Command.FILE_TRANSFER_REJECT_RESP, new FileTransferRejectRespHandler(mapper));
        this.commands.put(Command.FILE_TRANSFER_INIT, this::handleFileTransferInit);

    }

    public void start() {
        checkConnectionToServer();
        login();
        listenToServer();
        handleMenu();
    }

    public void listenToServer() {
        Thread serverListener = new Thread(() -> {
            try {
                String serverMessage;
                while (running && (serverMessage = serverReader.readLine()) != null) {

                    Message message = deserializeMessage(serverMessage);
                    String header = "";
                    String rawPayload = "";
                    if (message != null) {
                        header = message.getHeader();
                        rawPayload = message.getPayload();

                    }

                    Command command = Command.getCommandFromString(header);


                    this.commands.get(command).accept(rawPayload);


                }


            } catch (IOException e) {

                shutDown();
            }
        });

        serverListener.start();
    }

    private void checkConnectionToServer() {
        try {

            String serverMessage = serverReader.readLine();
            Message message = messageHandler.deserialize(serverMessage);

            String payload = message.getPayload();

            commands.get(Command.READY).accept(payload);

        } catch (IOException e) {
            System.err.println("Error reading READY message: " + e.getMessage());
            shutDown();
        }

    }

    private void shutDown() {
        close();
        System.exit(0);
    }


    public void printMenu() {
        System.out.println("Menu:");
        System.out.println("____________________________");
        System.out.println("1. Broadcast message");
        System.out.println("2. Request list of users");
        System.out.println("3. Send private message");
        System.out.println("4. Play Rock/Paper/Scissors game ");
        System.out.println("5. Make a choice for the Rock/Paper/Scissors game ");
        System.out.println("6. Send file transfer request");
        System.out.println("7. Accept file transfer");
        System.out.println("8. Reject file transfer ");
        System.out.println("0. Logout");


    }

    public void login() {

        System.out.println("Welcome to Chat Application!");
        System.out.println("To use the chat application, you need to login");
        try {
            while (!isLoggedIn) {
                System.out.print("Please enter a username: ");
                String username = reader.readLine().strip();

                if (username.isEmpty()) {
                    System.out.println("Username cannot be empty. Please try again.");
                    continue;

                }
                Enter payload = new Enter(username);
                String serializedPayload = mapper.writeValueAsString(payload);

                Message loginMessage = new Message(Command.ENTER.getCommand(), serializedPayload);
                String serializedLoginMessage = messageHandler.serialize(loginMessage);
                serverWriter.println(serializedLoginMessage);

                String serverResponse = serverReader.readLine();
                Message responseMessage = messageHandler.deserialize(serverResponse);

                String header = responseMessage.getHeader();
                String responsePayload = responseMessage.getPayload();


                if (header.equals(Command.UNKNOWN_COMMAND.getCommand())) {
                    commands.get(Command.UNKNOWN_COMMAND).accept(responsePayload);
                } else {

                    commands.get(Command.ENTER_RESP).accept(responsePayload);

                    if (responsePayload.contains("\"status\":\"OK\"")) {
                        isLoggedIn = true;
                    }

                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }


    public void handleMenu() {
        int choice = -1;
        while (choice != 0) {
            printMenu();
            try {
                System.out.print("Enter your choice: ");
                String userInput = reader.readLine().strip();
                choice = Integer.parseInt(userInput);

                switch (choice) {
                    case 1:
                        broadcastMessage();
                        break;


                    case 2:
                        requestListOfUsers();
                        break;

                    case 3:
                        sendPrivateMessage();
                        break;
                    case 4:
                        startGame();
                        break;
                    case 5:
                        makeMove();
                        break;

                    case 6:
                        sendFileTransferRequest();
                        break;

                    case 7:
                        handleFileTransferAcceptance();
                        break;
                    case 8:
                        handleFileTransferRejection();
                        break;
                    case 0:

                        leftChat();
                        break;
                    default:
                        System.out.println("Invalid choice. Please select option from the menu (0-5):");
                }

            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number (0-5):");
            }

        }
    }

    public void requestListOfUsers() {
        try {

            Message userListReqMessage = new Message(Command.USER_LIST_REQ.getCommand(), null);
            String serializedUserListReqMessage = messageHandler.serialize(userListReqMessage);
            serverWriter.println(serializedUserListReqMessage);


        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void sendPrivateMessage() {
        try {
            String recipient = "";

            while (recipient.isEmpty()) {
                System.out.println("Please enter the username of the recipient:");
                recipient = reader.readLine().trim();
                if (recipient.isEmpty()) {
                    System.out.println("Recipient username cannot be empty. Please try again.");

                }
            }

            String message = "";
            // Prompt user for a message until a valid one is entered
            while (message.isEmpty()) {
                System.out.println("Please enter the message to send:");
                message = reader.readLine().trim();
                if (message.isEmpty()) {
                    System.out.println("Message cannot be empty. Please try again.");

                }
            }
            PrivateMessageReq privateMessageReq = new PrivateMessageReq(recipient, message);
            String payload = mapper.writeValueAsString(privateMessageReq);

            Message privateMessage = new Message(Command.PRIVATE_MSG_REQ.getCommand(), payload);
            String serializedPrivateMessageRequest = messageHandler.serialize(privateMessage);


            serverWriter.println(serializedPrivateMessageRequest);


        } catch (IOException e) {
            System.err.println("Error sending private message: " + e.getMessage());
        }
    }


    public void broadcastMessage() {
        try {
            String message = "";
            while (message.isEmpty()) {
                System.out.println("Please enter a message to broadcast:");
                message = reader.readLine().trim();
                if (message.isEmpty()) {
                    System.out.println("Message for sending cannot be empty. Please try again.");

                }

            }

            BroadcastReq broadcastReq = new BroadcastReq(message);
            String payload = mapper.writeValueAsString(broadcastReq);
            Message broadcastMessage = new Message(Command.BROADCAST_REQ.getCommand(), payload);
            String serializedBroadcast = messageHandler.serialize(broadcastMessage);

            serverWriter.println(serializedBroadcast);

        } catch (IOException e) {
            System.err.println("Error during broadcast: " + e.getMessage());
        }
    }


    public void close() {
        try {
            if (socket != null) socket.close();
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (serverReader != null) serverReader.close();
            if (serverWriter != null) serverWriter.close();
            running = false;

        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }


    public void leftChat() {
        try {

            Message byeMessage = new Message(Command.BYE.getCommand(), null);
            String serializedByeMessage = messageHandler.serialize(byeMessage);
            serverWriter.println(serializedByeMessage);


        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void startGame() {
        try {
            String opponent = "";
            while (opponent.isEmpty()) {
                System.out.println("Please enter a username of the opponent you want to play with:");
                opponent = reader.readLine().trim();
                if (opponent.isEmpty()) {
                    System.out.println("Opponent username for game cannot be empty. Please try again.");

                }

            }

            GameStartReq gameStartReq = new GameStartReq(opponent);
            String payload = mapper.writeValueAsString(gameStartReq);
            Message gameStartReqMessage = new Message(Command.GAME_START_REQ.getCommand(), payload);
            String serializedGameStartReq = messageHandler.serialize(gameStartReqMessage);

            serverWriter.println(serializedGameStartReq);

        } catch (IOException e) {
            System.err.println("Error during sending game start request: " + e.getMessage());
        }
    }

    private void makeMove() {
        try {
            String move = "";
            while (move.isEmpty()) {
                System.out.println("Possible moves: R-Rock, P-Paper, S-Scissors. Please type a letter for move you selected:");
                move = reader.readLine().trim().toUpperCase();
                if (move.isEmpty()) {
                    System.out.println("Move for game cannot be empty. Please try again.");
                }
            }

            GameMoves gameMove = GameMoves.fromCode(move);
            sendMoveMessage(gameMove);


        } catch (IOException e) {
            System.err.println("Error during sending game move: " + e.getMessage());
        }


    }

    private void sendMoveMessage(GameMoves move) {
        try {
            String moveCode = (move != null) ? move.getCode() : null;
            GameMove gameMovePayload = new GameMove(moveCode);
            String payload = mapper.writeValueAsString(gameMovePayload);
            Message gameMoveMessage = new Message(Command.GAME_MOVE.getCommand(), payload);
            String serializedGameMoveMessage = messageHandler.serialize(gameMoveMessage);
            serverWriter.println(serializedGameMoveMessage);


        } catch (IOException e) {
            System.err.println("Error during sending game move message: " + e.getMessage());
        }
    }

    private void sendFileTransferRequest() {
        try {
            String receiver = "";
            while (receiver.isEmpty()) {
                System.out.println("Please enter the username of the user you want send file to:");
                receiver = reader.readLine().trim();
                if (receiver.isEmpty()) {
                    System.out.println("Username of receiver user cannot be empty. Please try again.");
                }
            }
            String filePath = "";
            while (filePath.isEmpty()) {
                System.out.println("Please enter the full path to file you want to transfer:");
                filePath = reader.readLine().trim();
                if (filePath.isEmpty()) {
                    System.out.println("Path to file cannot be empty. Please try again.");
                }
            }
            this.filePath = filePath;


            File file = new File(filePath);
            long fileSize = file.length();
            String fileHash = calculateSHA256Hash(filePath);
            // Get filename from the File object
            String fileName = file.getName();


            FileTransferReq fileTransferReq = new FileTransferReq(receiver, fileName, fileSize, fileHash);
            String payload = mapper.writeValueAsString(fileTransferReq);
            Message fileTransferRequestMessage = new Message(Command.FILE_TRANSFER_REQ.getCommand(), payload);
            String serializedMessage = messageHandler.serialize(fileTransferRequestMessage);
            serverWriter.println(serializedMessage);


        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error during sending file transfer request:" + e.getMessage());
        }

    }

    public static String calculateSHA256Hash(String filePath) throws NoSuchAlgorithmException, IOException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();

    }

    private void receiveFileTransferRequest(String payload) {
        try {
            FileTransferReq request = mapper.readValue(payload, FileTransferReq.class);
            String sender = request.receiverOrSender();
            String fileName = request.filename();
            long fileSize = request.fileSize();
            String checksum = request.checksum();
            System.out.println(sender + " wants to send you the file \"" + fileName + "\" with a size of " + fileSize + " bytes.");
            this.checksum = checksum;
            this.fileName = fileName;
            System.out.println("To accept, select option 7 from the menu; to reject, select option 8 from the menu.");


        } catch (JsonProcessingException e) {
            System.err.println("Failed to process the file transfer request: " + e.getMessage());
        }

    }


    public void clearFilePath() {
        this.filePath = "";
    }

    private void handleFileTransferAcceptance() {
        try {
            String sender = "";
            while (sender.isEmpty()) {
                System.out.println("Please enter the username of the user you want to accept file transfer from:");
                sender = reader.readLine().trim();
                if (sender.isEmpty()) {
                    System.out.println("Username of  user cannot be empty. Please try again.");
                }
            }

            FileTransferAccept fileTransferAccept = new FileTransferAccept(sender);
            String serializedPayload = mapper.writeValueAsString(fileTransferAccept);
            Message fileTransferAcceptMessage = new Message(Command.FILE_TRANSFER_ACCEPT.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferAcceptMessage);
            serverWriter.println(serializedMessage);


        } catch (IOException e) {
            System.err.println("Error during sending file transfer acceptance: " + e.getMessage());
        }
    }

    private void handleFileTransferRejection() {
        try {
            String sender = "";
            while (sender.isEmpty()) {
                System.out.println("Please enter the username of the user you want to reject file transfer from:");
                sender = reader.readLine().trim();
                if (sender.isEmpty()) {
                    System.out.println("Username of  user cannot be empty. Please try again.");
                }
            }

            FileTransferReject fileTransferReject = new FileTransferReject(sender);
            String serializedPayload = mapper.writeValueAsString(fileTransferReject);
            Message fileTransferRejectMessage = new Message(Command.FILE_TRANSFER_REJECT.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferRejectMessage);
            serverWriter.println(serializedMessage);


        } catch (IOException e) {
            System.err.println("Error during sending file transfer rejection: " + e.getMessage());
        }
    }

    private void handleFileTransferInit(String payload) {
        String uuid = "";
        try {
            FileTransferInit fileTransferInit = mapper.readValue(payload, FileTransferInit.class);
            uuid = fileTransferInit.uuid();

        } catch (JsonProcessingException e) {
            System.err.println("Failed to process the file transfer init: " + e.getMessage());
        }

        if (!filePath.isEmpty()) {
            ClientFileSender sender = new ClientFileSender(filePath, uuid, HOST, FILE_TRANSFER_PORT);
            new Thread(sender).start();
        } else {
            ClientFileReceiver receiver = new ClientFileReceiver(checksum, uuid, fileName, HOST, FILE_TRANSFER_PORT);
            new Thread(receiver).start();

        }


    }


    private Message deserializeMessage(String rawMessage) {

        try {
            return messageHandler.deserialize(rawMessage);
        } catch (Exception e) {
            System.err.println("Failed to deserialize message: " + e.getMessage());
            return null;
        }
    }


}
