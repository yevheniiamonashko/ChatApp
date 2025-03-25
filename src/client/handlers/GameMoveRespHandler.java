package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.GameMoveResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.util.function.Consumer;

public class GameMoveRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public GameMoveRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {
            GameMoveResp gameMoveResponse = mapper.readValue(payload, GameMoveResp.class);
            Status status = Status.valueOf(gameMoveResponse.status().toUpperCase());
            switch (status) {
                case OK:
                    System.out.println("Your move was sent successfully. Please wait for game result.");

                    break;

                case ERROR:
                    int errorCode = gameMoveResponse.code();
                    Code code = Code.fromCode(errorCode);

                    if (code != null) {
                        ErrorHandler.handleResponseErrors(code);
                    } else {
                        ErrorHandler.handleUnknownErrorCode(errorCode);
                    }
                    break;
            }


        } catch (Exception e) {
            System.err.println("Failed to process game move message: " + e.getMessage());
        }
    }

}
