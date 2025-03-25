package server;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.GameMoves;
import shared.utility.MessageHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private static final int SERVER_PORT = 1337;
    private static final int FILE_TRANSFER_PORT = 1338;
    private ServerSocket serverSocket;
   private final Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> loggedInClients = new ConcurrentHashMap<>();
    private static final String SERVER_VERSION = "1.6.0";
    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageHandler messageHandler = new MessageHandler();



    private ClientHandler playerA;
    private ClientHandler playerB;
    private String moveA;
    private String moveB;
    private boolean gameActive = false;
    private Timer moveTimeoutTimer; // Timer for move submission
    private static final int MOVE_TIMEOUT_MS = 60 * 1000;


    private boolean running = true;

    private FileTransferServer fileTransferServer;

    public static void main(String[] args) {
        Server server = new Server();
        new Thread(server::start).start();
        new Thread(server::startFileTransferServer).start();
    }

    public void start() {
        try {


            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port " + SERVER_PORT);
            System.out.println("Type '0' to stop the server.");

            new Thread(this::listenForStopCommand).start();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this, SERVER_VERSION);
               clients.put(clientSocket, clientHandler);
                new Thread(clientHandler).start();


            }


        } catch (IOException e) {
            if (running) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }


   public void startFileTransferServer(){
        fileTransferServer = new FileTransferServer(FILE_TRANSFER_PORT);
        new Thread(fileTransferServer).start();
    }



    public Map<String, ClientHandler> getLoggedInUsers() {
        return loggedInClients;
    }

    public void addNewUser(String username, ClientHandler clientHandler) {
        loggedInClients.put(username, clientHandler);
    }

    public void removeUser(String username, Socket clientSocket) {
        loggedInClients.remove(username);
       // clients.remove(clientSocket);
        System.out.println("User removed: " + username);
    }

    public void sendJoinedMessageToAllUsers(String username) {
        try {

            Joined payload = new Joined(username);
            String serializedPayload = mapper.writeValueAsString(payload);
            Message joinedMessage = new Message(Command.JOINED.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(joinedMessage);

            // Send the message to all clients
            for (ClientHandler clientHandler : loggedInClients.values()) {
                if (!username.equals(clientHandler.getUsername())) { // Skip the client that just joined
                    clientHandler.getWriter().println(serializedMessage);
                }
            }

            System.out.println("Sent JOINED message to all clients: " + username);

        } catch (IOException e) {
            System.err.println("Error sending JOINED message: " + e.getMessage());
        }
    }


    public void sendLeftMessageToAllUsers(String username) {

        if (loggedInClients.isEmpty()) {
            System.out.println("No clients left to notify about user leaving: " + username);
            return;
        }

        try {

            Left payload = new Left(username);
            String serializedPayload = mapper.writeValueAsString(payload);
            Message leftMessage = new Message(Command.LEFT.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(leftMessage);


            for (ClientHandler clientHandler : loggedInClients.values()) {
                clientHandler.getWriter().println(serializedMessage);

            }


            System.out.println("Sent LEFT message to all clients: " + username);

        } catch (IOException e) {
            System.err.println("Error sending LEFT message: " + e.getMessage());
        }
    }

    public void sendBroadcastMessageToAllUsers(String username, String message) {
        try {

            Broadcast payload = new Broadcast(username, message);
            String serializedPayload = mapper.writeValueAsString(payload);
            Message broadcastMessage = new Message(Command.BROADCAST.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(broadcastMessage);

            // Send the message to all clients
            for (ClientHandler clientHandler : loggedInClients.values()) {
                if (!username.equals(clientHandler.getUsername())) { // Skip the client that just joined
                    clientHandler.getWriter().println(serializedMessage);
                }
            }

            System.out.println("Sent BROADCAST message to all clients: " + username);

        } catch (IOException e) {
            System.err.println("Error sending BROADCAST message: " + e.getMessage());
        }

    }

    public void sendPrivateMessageToSpecificUser(String sender, String recipient, String message) {
        try {
            ClientHandler recepinetClientHandler = loggedInClients.get(recipient);
            PrivateMessage privateMessage = new PrivateMessage(sender, message);
            String serializedPayload = mapper.writeValueAsString(privateMessage);
            Message privateMsgMessage = new Message(Command.PRIVATE_MSG.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(privateMsgMessage);
            recepinetClientHandler.getWriter().println(serializedMessage);
            System.out.println("Sent private message to client: " + recipient);


        } catch (IOException e) {
            System.err.println("Error sending private message: " + e.getMessage());
        }


    }


    private void listenForStopCommand() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            String input = scanner.nextLine().strip();
            if (input.equalsIgnoreCase("0")) {
                System.out.println("Stopping the server...");
                stop();
                break;
            }
        }
        scanner.close();
    }

    private void stop() {
        try {
            running = false;
            if (!loggedInClients.isEmpty()) {
                for (ClientHandler clientHandler : loggedInClients.values()) {
                    clientHandler.getPongManager().stopPingProcess();
                }
            }

            if (moveTimeoutTimer != null) {
                moveTimeoutTimer.cancel();
                moveTimeoutTimer = null;
            }

            if (gameActive) {
                resetGame();
            }

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed.");
            }
            fileTransferServer.setRunning(running);
            fileTransferServer.getFileTransferSocket().close();

            System.exit(0);

        } catch (IOException e) {
            System.err.println("Error while stopping the server: " + e.getMessage());
        }
    }


    public synchronized void startGame(String initiator, String opponent) {
        ClientHandler initiatorHandler = loggedInClients.get(initiator);
        ClientHandler opponentHandler = loggedInClients.get(opponent);
        this.playerA = initiatorHandler;
        this.playerB = opponentHandler;
        this.gameActive = true;
        notifyBothUsersAboutGame(opponent, initiator);
        startMoveTimeout();
    }

    public synchronized List<String> checkIfGameActiveAndGetPlayers() {
        if (!gameActive) {
            return Collections.emptyList();
        }
        return Arrays.asList(playerA.getUsername(), playerB.getUsername());
    }

    public void sendGameInvite(String opponent, String initiator) {
        try {
            ClientHandler opponentClientHandler = loggedInClients.get(opponent);
            GameInvite gameInvite = new GameInvite(initiator);
            String serializedPayload = mapper.writeValueAsString(gameInvite);
            Message gameInviteMessage = new Message(Command.GAME_INVITATION.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(gameInviteMessage);
            opponentClientHandler.getWriter().println(serializedMessage);
            System.out.println("Sent game invite to: " + opponent);

            startGame(initiator, opponent);


        } catch (IOException e) {
            System.err.println("Error sending game invite: " + e.getMessage());
        }
    }

    public void notifyBothUsersAboutGame(String opponent, String initiator) {
        try {
            ClientHandler opponentClientHandler = loggedInClients.get(opponent);
            ClientHandler initiatorClientHandler = loggedInClients.get(initiator);

            GameNotification gameNotification = new GameNotification(initiator, opponent);
            String serializedPayload = mapper.writeValueAsString(gameNotification);
            Message gameNotificationMessage = new Message(Command.GAME_NOTIFICATION.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(gameNotificationMessage);
            opponentClientHandler.getWriter().println(serializedMessage);
            initiatorClientHandler.getWriter().println(serializedMessage);

            System.out.println("Sent game  notification to users");


        } catch (IOException e) {
            System.err.println("Error sending game notification: " + e.getMessage());
        }
    }


    private synchronized void resetGame() {
        playerA = null;
        playerB = null;
        moveA = null;
        moveB = null;
        gameActive = false;


    }

    public synchronized boolean isGameActive() {
        return gameActive;
    }

    // Handle user disconnection during an active game
    public synchronized void handleGameDisconnection(ClientHandler disconnectedClient, String username) {
        if (!gameActive) {
            return;
        }


        boolean isPlayerA = playerA == disconnectedClient;
        boolean isPlayerB = playerB == disconnectedClient;

        if (isPlayerA || isPlayerB) {
            System.out.println("Player " + username + " disconnected during the game.");


            ClientHandler remainingPlayer = isPlayerA ? playerB : playerA;


            if (remainingPlayer != null) {
                sendGameCancellation(remainingPlayer, Code.OPPONENT_DISCONNECTED.getCode()); // Opponent disconnected
            }


            resetGame();
        }


    }


    public void sendGameCancellation(ClientHandler participant, int errorCode) {
        try {
            GameCancelled gameCancelledPayload = new GameCancelled(errorCode);
            String serializedPayload = mapper.writeValueAsString(gameCancelledPayload);

            Message cancellationMessage = new Message(Command.GAME_CANCELLED.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(cancellationMessage);

            participant.getWriter().println(serializedMessage);
            System.out.println("Notified " + participant.getUsername() + " about game cancellation.");
        } catch (IOException e) {
            System.err.println("Error notifying game cancellation: " + e.getMessage());
        }
    }

    public boolean isGameParticipant(ClientHandler participant) {

        return participant == playerA || participant == playerB;
    }

    public synchronized boolean hasMoveBeenSubmitted(ClientHandler participant) {
        if (participant == playerA) {
            return moveA != null; // Check if player A has submitted a move
        } else if (participant == playerB) {
            return moveB != null; // Check if player B has submitted a move
        }
        return false;
    }

    public synchronized void submitMove(ClientHandler participant, GameMoves move) {

        if (participant == playerA) {
            moveA = move.getCode();
        } else if (participant == playerB) {
            moveB = move.getCode();
        }


        if (moveA != null && moveB != null) {
            if (moveTimeoutTimer != null) {
                moveTimeoutTimer.cancel();
            }
            determineWinner();
        }
    }

    public void determineWinner() {
        String winner = null;

        if (!moveA.equals(moveB)) {
            winner = (moveA.equals("R") && moveB.equals("S")) ||
                    (moveA.equals("S") && moveB.equals("P")) ||
                    (moveA.equals("P") && moveB.equals("R")) ? playerA.getUsername() : playerB.getUsername();
        }

        sendGameResultMessage(winner);
    }

    public void sendGameResultMessage(String winner) {
        try {
            GameResult gameResult = new GameResult(winner, moveA, moveB);
            String serializedPayload = mapper.writeValueAsString(gameResult);
            Message gameResultMessage = new Message(Command.GAME_RESULT.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(gameResultMessage);
            playerA.getWriter().println(serializedMessage);
            playerB.getWriter().println(serializedMessage);
            resetGame();
        } catch (IOException e) {
            System.err.println("Error sending game result: " + e.getMessage());
        }
    }

    private synchronized void startMoveTimeout() {
        if (moveTimeoutTimer != null) {
            moveTimeoutTimer.cancel();
        }

        moveTimeoutTimer = new Timer();
        moveTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (Server.this) {
                    if ((moveA == null || moveB == null) && gameActive) {

                        cancelGameDueToTimeout();

                    }
                }
            }
        }, MOVE_TIMEOUT_MS);
    }

    private synchronized void cancelGameDueToTimeout() {
        if (!gameActive) return;


        if (playerA != null) {
            sendGameCancellation(playerA, Code.MOVES_OR_MOVE_NOT_SUBMITTED.getCode());
        }
        if (playerB != null) {
            sendGameCancellation(playerB, Code.MOVES_OR_MOVE_NOT_SUBMITTED.getCode());
        }


        resetGame();
    }

    public void sendFileTransferRequest(String sender, String receiver, String filename, long fileSize, String checksum) {
        try {
            ClientHandler receivingClient = loggedInClients.get(receiver);
            FileTransferReq fileTransferReq = new FileTransferReq(sender, filename, fileSize, checksum);
            String serializedPayload = mapper.writeValueAsString(fileTransferReq);
            Message fileTransferRequestMessage = new Message(Command.FILE_TRANSFER_REQ.getCommand(), serializedPayload);
            String serializedFileTransferRequestMessage = messageHandler.serialize(fileTransferRequestMessage);
            receivingClient.getWriter().println(serializedFileTransferRequestMessage);
        } catch (IOException e) {
            System.err.println("Error sending file transfer request: " + e.getMessage());
        }
    }

    public void sendFileTransferRejection(String sender) {
        try {
            ClientHandler fileTransferInitiator = loggedInClients.get(sender);
            FileTransferResp fileTransferResp = FileTransferResp.error();
            String serializedPayload = mapper.writeValueAsString(fileTransferResp);
            Message fileTransferResponseMessage = new Message(Command.FILE_TRANSFER_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferResponseMessage);
            fileTransferInitiator.getWriter().println(serializedMessage);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending file transfer response about rejection: " + e.getMessage());
        }
    }

    public void sendFileTransferConfirmation(String sender, String receiver) {
        try {
            ClientHandler fileTransferInitiator = loggedInClients.get(sender);
            FileTransferResp fileTransferResp = FileTransferResp.success();
            String serializedPayload = mapper.writeValueAsString(fileTransferResp);
            Message fileTransferResponseMessage = new Message(Command.FILE_TRANSFER_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferResponseMessage);
            fileTransferInitiator.getWriter().println(serializedMessage);
            generateUUIDAndSendFileTransferInitMessage(sender, receiver);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending file transfer response about acceptance: " + e.getMessage());
        }

    }

    private void generateUUIDAndSendFileTransferInitMessage(String sender, String receiver) {
        String sessionId = UUID.randomUUID().toString();
        ClientHandler senderClient = loggedInClients.get(sender);
        ClientHandler receiverClient = loggedInClients.get(receiver);
        try {
            FileTransferInit fileTransferInit = new FileTransferInit(sessionId);
            String serializedPayload = mapper.writeValueAsString(fileTransferInit);
            Message fileTransferInitMessage = new Message(Command.FILE_TRANSFER_INIT.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferInitMessage);
            senderClient.getWriter().println(serializedMessage);
            receiverClient.getWriter().println(serializedMessage);


        } catch (JsonProcessingException e) {
            System.err.println("Error sending file transfer init message: " + e.getMessage());
        }
    }


}
