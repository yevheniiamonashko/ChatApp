package server;

import shared.utility.model.Message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.handlers.*;
import shared.messages.*;
import shared.utility.Command;
import shared.utility.GameMoves;
import shared.utility.MessageHandler;
import shared.utility.Status;

import java.io.*;
import java.net.Socket;
import java.util.*;

import java.util.function.Consumer;

public class ClientHandler implements Runnable {

    // client socket and server instance
    private final Socket clientSocket;

    private PrintWriter writer;
    private BufferedReader reader;

    private String username;
    private final Server server;
    private final MessageHandler messageHandler;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Command, Consumer<String>> commands = new HashMap<>();
    private PongManager pongManager;


    public ClientHandler(Socket clientSocket, Server server, String serverVersion) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.messageHandler = new MessageHandler();


        if (setup()) {

            putCommandsForMessagesFromServerIntoMap();
            sendReadyMessage(serverVersion);
            this.pongManager = new PongManager(this, mapper, writer, messageHandler);

        } else {
            System.err.println("Failed to set up streams for client: " + clientSocket.getInetAddress());
            close();
        }

    }

    private void putCommandsForMessagesFromServerIntoMap() {

        this.commands.put(Command.ENTER, new EnterHandler(mapper, this, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.BYE, payload -> logoutUser());
        this.commands.put(Command.BROADCAST_REQ, new BroadcastReqHandler(mapper, this, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.USER_LIST_REQ, new UserListReqHandler(mapper, this, writer, messageHandler));
        this.commands.put(Command.PRIVATE_MSG_REQ, new PrivateMsgReqHandler(mapper, this, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.GAME_START_REQ, new GameStartReqHandler(this, mapper, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.GAME_MOVE, new GameMoveHandler(mapper, this, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.FILE_TRANSFER_REQ, new FileTransferReqHandler(this, mapper, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.FILE_TRANSFER_ACCEPT, new FileTransferAcceptHandler(mapper, this, writer, messageHandler, this::handleParseError));
        this.commands.put(Command.FILE_TRANSFER_REJECT, new FileTransferRejectHandler(mapper, this, writer, messageHandler, this::handleParseError));

    }

    public void sendListOfUsersToUser() {

        ArrayList<String> users = new ArrayList<>(server.getLoggedInUsers().keySet());

        users.remove(this.username);
        try {


            UserListResp userListRespMessage = UserListResp.success(users);
            String serializedPayload = mapper.writeValueAsString(userListRespMessage);


            Message userListSuccessMessage = new Message(Command.USER_LIST_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(userListSuccessMessage);

            writer.println(serializedMessage);


        } catch (IOException e) {
           System.err.println("Error serializing user list response: " + e.getMessage());
        }


    }


    public void sendPrivateMessage(String sender, String recipient, String message) {
        try {


            PrivateMessageResp privateMessageRespMessage = PrivateMessageResp.success();
            String serializedPayload = mapper.writeValueAsString(privateMessageRespMessage);
            Message privateMsgSuccessMessage = new Message(Command.PRIVATE_MSG_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(privateMsgSuccessMessage);
            writer.println(serializedMessage);

            server.sendPrivateMessageToSpecificUser(sender, recipient, message);


        } catch (IOException e) {
           System.err.println("Error serializing private message response: " + e.getMessage());
        }

    }

    public PrintWriter getWriter() {
        return writer;
    }

    @Override
    public void run() {
        try {
            String input;
            while ((input = reader.readLine()) != null) {
                processMessage(input);
            }
            close();

            server.removeUser(username, clientSocket);

        } catch (IOException e) {

            close();
        }
    }


    private void processMessage(String input) {
        try {
            Message clientMessage = messageHandler.deserialize(input);
            String header = "";
            String payload = "";
            if (clientMessage != null) {
                header = clientMessage.getHeader();
                payload = clientMessage.getPayload();

                if (!header.equals(Command.PONG.getCommand()) &&
                        !header.equals(Command.BYE.getCommand()) &&
                        !header.equals(Command.USER_LIST_REQ.getCommand())) {
                    // If the payload is empty, set it to "{}"
                    if (payload.isEmpty()) {
                        payload = "{}";
                    }
                }


            }

            Command command = Command.getCommandFromString(header);
            if (command == null) {
                handleUnknownCommand();
                return;
            }

            if (command == Command.PONG) {
                pongManager.processPongCommand();
                return;
            }


            this.commands.get(command).accept(payload);

        } catch (JsonProcessingException e) {
            handleParseError();
        }
    }

    private boolean setup() {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));
            writer = new PrintWriter(outputStream, true);
            return true;
        } catch (IOException e) {
            System.err.println("Unable to establish the client connection: " + e.getMessage());
            return false;
        }
    }

    private void sendReadyMessage(String serverVersion) {
        try {
            // Create the payload
            Ready readyPayload = new Ready(serverVersion);
            String serializedPayload = mapper.writeValueAsString(readyPayload);

            // Create the full READY message
            Message readyMessage = new Message(Command.READY.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(readyMessage);

            // Send the READY message to the client
            writer.println(serializedMessage);
            System.out.println("Sent READY message to client: " + clientSocket.getInetAddress());

        } catch (IOException e) {
            System.err.println("Error sending READY message: " + e.getMessage());
            close();
        }
    }


    public boolean checkIfClientExists(String username) {
        if (username == null) {
            return false;
        }
        return server.getLoggedInUsers().containsKey(username);
    }


    public void loginUser(String username) {
        try {
            this.username = username;


            EnterResp successLoginPayload = EnterResp.success();
            String serializedPayload = mapper.writeValueAsString(successLoginPayload);

            // Create the full READY message
            Message loginSuccessMessage = new Message(Command.ENTER_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(loginSuccessMessage);

            writer.println(serializedMessage);

            server.addNewUser(this.username, this);
            server.sendJoinedMessageToAllUsers(username);


            pongManager.startPingProcess();


        } catch (IOException e) {
            System.err.println("Error logging in: " + e.getMessage());
        }

    }


    private void logoutUser() {
        try {
            ByeResp byeResp = new ByeResp(Status.OK.getStatus());
            String payload = mapper.writeValueAsString(byeResp);
            Message byeResponseMessage = new Message(Command.BYE_RESP.getCommand(), payload);
            String serializedMessage = messageHandler.serialize(byeResponseMessage);
            writer.println(serializedMessage);
            writer.flush();


            pongManager.stopPingProcess();
            server.removeUser(this.username, this.clientSocket);
            server.sendLeftMessageToAllUsers(this.username);
            server.handleGameDisconnection(this, username);
        } catch (JsonProcessingException e) {
            System.err.println("Error sending bye response message: " + e.getMessage());
        }

    }


    private void close() {
        try {

            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null) clientSocket.close();

        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }


    public void handleDisconnection() {
        try {
            pongManager.stopPingProcess();
            server.removeUser(username, clientSocket);
            close();
        } catch (Exception e) {
            System.err.println("Error handling disconnection for client: " + username);
        }
    }

    private void handleUnknownCommand() {
        try {
            Message unknownCommandMessage = new Message(Command.UNKNOWN_COMMAND.getCommand(), null);
            String serializedMessage = messageHandler.serialize(unknownCommandMessage);
            writer.println(serializedMessage);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending message about unknown command: " + e.getMessage());
        }


    }

    private void handleParseError() {
        try {

            Message parseErrorMessage = new Message(Command.PARSE_ERROR.getCommand(), null);
            String serializedMessage = messageHandler.serialize(parseErrorMessage);

            writer.println(serializedMessage);
            System.err.println("Sent PARSE_ERROR to client: " + username);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending PARSE_ERROR to client: " + e.getMessage());
        }
    }

    public void broadcastMessage(String username, String message) {
        try {

            BroadcastResp successBroadcastResponsePayload = BroadcastResp.success();
            String serializedPayload = mapper.writeValueAsString(successBroadcastResponsePayload);


            Message broadcastSuccessMessage = new Message(Command.BROADCAST_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(broadcastSuccessMessage);
            writer.println(serializedMessage);

            server.sendBroadcastMessageToAllUsers(username, message);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending message about broadcast response: " + e.getMessage());
        }
    }

    public void sentSuccessGameStartResponse(String opponent) {
        try {
            GameStartResp successGameStartResponsePayload = GameStartResp.success();
            String serializedPayload = mapper.writeValueAsString(successGameStartResponsePayload);
            Message gameStartSuccessMessage = new Message(Command.GAME_START_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(gameStartSuccessMessage);
            writer.println(serializedMessage);
            server.sendGameInvite(opponent, this.username);

        } catch (JsonProcessingException e) {
            System.out.println("Error during sending game start response: " + e.getMessage());
        }
    }


    public String getUsername() {
        return username;
    }

    public PongManager getPongManager() {
        return pongManager;
    }

    public List<String> getGamePlayers() {
        return server.checkIfGameActiveAndGetPlayers();
    }

    public boolean checkIfGameActive() {
        return server.isGameActive();
    }

    public boolean checkIfClientGameParticipant() {
        return server.isGameParticipant(this);
    }

    public boolean isMoveAlreadySubmitted() {
        return server.hasMoveBeenSubmitted(this);
    }

    public void sendMoveResponseAndSubmitMove(GameMoves move) {
        try {
            // Notify the player about successful move submission
            GameMoveResp successResponse = GameMoveResp.success();
            String serializedPayload = mapper.writeValueAsString(successResponse);
            Message successMessage = new Message(Command.GAME_MOVE_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(successMessage);
            writer.println(serializedMessage);

            // Submit the move to the server
            server.submitMove(this, move);

        } catch (Exception e) {
            System.err.println("Error processing move submission: " + e.getMessage());
        }
    }

    public void sendFileTransferRequestToReceiver(String receiver, String filename, long fileSize, String checksum) {
        server.sendFileTransferRequest(username, receiver, filename, fileSize, checksum);
    }

    public void handleSuccessfulFileTransferAccept(String sender) {
        try {
            FileTransferAcceptResp successResponse = FileTransferAcceptResp.success();
            String serializedPayload = mapper.writeValueAsString(successResponse);
            Message fileTransferSuccessAcceptmessage = new Message(Command.FILE_TRANSFER_ACCEPT_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferSuccessAcceptmessage);
            writer.println(serializedMessage);
            server.sendFileTransferConfirmation(sender, this.username);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending file transfer successful accept response: " + e.getMessage());
        }

    }

    public void handleSuccessfulFileTransferReject(String sender) {
        try {
            FileTransferRejectResp successResponse = FileTransferRejectResp.success();
            String serializedPayload = mapper.writeValueAsString(successResponse);
            Message fileTransferSuccessRejectmessage = new Message(Command.FILE_TRANSFER_REJECT_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferSuccessRejectmessage);
            writer.println(serializedMessage);
            server.sendFileTransferRejection(sender);

        } catch (JsonProcessingException e) {
            System.err.println("Error sending file transfer successful reject response: " + e.getMessage());
        }


    }


}