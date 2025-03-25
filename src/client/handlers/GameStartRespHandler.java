package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.EnterResp;
import shared.messages.GameStartResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.io.IOException;
import java.util.function.Consumer;

public class GameStartRespHandler implements Consumer<String> {

    private final ObjectMapper mapper;

    public GameStartRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;

    }

    @Override
    public void accept(String payload) {
        try {

            GameStartResp gameStartResp = mapper.readValue(payload, GameStartResp.class);


            Status status = Status.valueOf(gameStartResp.status().toUpperCase());

            switch (status) {
                case OK:

                    break;


                case ERROR:
                    int errorCode = gameStartResp.code();
                    Code code = Code.fromCode(errorCode);

                    if (code != null) {
                        ErrorHandler.handleResponseErrors(code);
                    } else {
                        ErrorHandler.handleUnknownErrorCode(errorCode);
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status in response: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to process game start response : " + e.getMessage());
        }
    }


}
