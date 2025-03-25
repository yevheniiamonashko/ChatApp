package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.GameStartReq;
import shared.messages.GameStartResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;

public class GameStartReqHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;

    public GameStartReqHandler(ClientHandler clientHandler, ObjectMapper mapper, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;
        this.mapper = mapper;
        this.clientHandler = clientHandler;


    }

    @Override
    public void accept(String payload) {
        try {
            GameStartReq gameStartReq = mapper.readValue(payload, GameStartReq.class);
            String opponent = gameStartReq.opponent();
            String initiator = clientHandler.getUsername();
            if (!clientHandler.checkIfClientExists(initiator)) {
                sendGameStartReqErrorMessage(Code.UNAUTHORIZED);
                return;
            }


            if (!clientHandler.checkIfClientExists(opponent)) {
                sendGameStartReqErrorMessage(Code.NOT_FOUND);
                return;

            }

            List<String> players = clientHandler.getGamePlayers();
            if (!players.isEmpty()) {
                sendGameStartReqErrorMessageWithUsers(players.get(0), players.get(1));
                return;
            }

            clientHandler.sentSuccessGameStartResponse(opponent);


        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }
    }

    private void sendGameStartReqErrorMessage(Code code) {
        try {

            GameStartResp errorResponse = GameStartResp.error(code.getCode());

            String serializedPayload = mapper.writeValueAsString(errorResponse);
            Message message = new Message(Command.GAME_START_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(message);
            writer.println(serializedMessage);


        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void sendGameStartReqErrorMessageWithUsers(String playerA, String playerB) {
        try {
            GameStartResp errorResponse = GameStartResp.errorWithUsers(Code.GAME_ALREADY_RUNNING.getCode(), playerA, playerB);

            String serializedPayload = mapper.writeValueAsString(errorResponse);
            Message message = new Message(Command.GAME_START_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(message);
            writer.println(serializedMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
}
