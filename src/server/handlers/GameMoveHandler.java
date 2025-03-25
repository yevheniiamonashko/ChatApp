package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.GameMove;
import shared.messages.GameMoveResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.GameMoves;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class GameMoveHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;

    public GameMoveHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;

    }

    @Override
    public void accept(String payload) {
        try {
            GameMove gameMove = mapper.readValue(payload, GameMove.class);
            GameMoves move = GameMoves.fromCode(gameMove.moveCode());
            if (move == null) {
                sendGameMoveErrorResponse(Code.INVALID_GAME_MOVE.getCode());
                return;
            }
            if (!clientHandler.checkIfGameActive()) {
                sendGameMoveErrorResponse(Code.NO_ACTIVE_GAME.getCode());
                return;
            }
            if (!clientHandler.checkIfClientGameParticipant()) {
                sendGameMoveErrorResponse(Code.USER_NOT_PARTICIPANT.getCode());
                return;
            }
            if (clientHandler.isMoveAlreadySubmitted()) {
                sendGameMoveErrorResponse(Code.MOVE_ALREADY_MADE.getCode());
                return;
            }
            clientHandler.sendMoveResponseAndSubmitMove(move);

        } catch (Exception e) {
            parseErrorHandler.handleParseError();
        }


    }

    private void sendGameMoveErrorResponse(int errorCode) {
        try {
            GameMoveResp errorResponse = GameMoveResp.error(errorCode);
            String serializedPayload = mapper.writeValueAsString(errorResponse);
            Message errorMessage = new Message(Command.GAME_MOVE_RESP.getCommand(), serializedPayload);
            writer.println(messageHandler.serialize(errorMessage));
        } catch (Exception e) {
            System.err.println("Error sending error response: " + e.getMessage());
        }
    }
}
