package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.GameCancelled;
import shared.utility.Code;
import shared.utility.ErrorHandler;

import java.io.IOException;
import java.util.function.Consumer;

public class GameCancelledHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public GameCancelledHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try{
            GameCancelled gameCancelled = mapper.readValue(payload, GameCancelled.class);
            int errorCode = gameCancelled.errorCode();
            Code code = Code.fromCode(errorCode);
            System.out.println("Your game was cancelled for the reason below. Please start a new game if you would like to play.");

            if (code != null) {
                ErrorHandler.handleResponseErrors(code);
            } else {
                ErrorHandler.handleUnknownErrorCode(errorCode);
            }

        }catch(IOException e) {
          System.err.println("Failed to process game cancellation message: "+e.getMessage());
        }

    }
}
