package client.handlers;



import shared.utility.Code;
import shared.utility.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.PongError;

import java.io.IOException;
import java.util.function.Consumer;

public class PongErrorHandler implements Consumer<String> {
    private final ObjectMapper mapper;



    public PongErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;

    }
    @Override
    public void accept(String payload) {
        try {

            PongError pongErrorMessage = mapper.readValue(payload, PongError.class);

            int errorCode = pongErrorMessage.code();
            Code code = Code.fromCode(errorCode);

            if (code != null) {
                ErrorHandler.handleResponseErrors(code);
            } else {
                ErrorHandler.handleUnknownErrorCode(errorCode);
            }




        } catch (IOException e) {
            System.err.println("Failed to process pong error message: " + e.getMessage());
        }

    }
}
