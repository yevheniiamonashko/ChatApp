package client.handlers;

import shared.utility.Code;
import shared.utility.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Hangup;

import java.io.IOException;
import java.util.function.Consumer;

public class HangupHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final Runnable closeConnection;


    public HangupHandler(ObjectMapper mapper, Runnable closeConnection) {
        this.mapper = mapper;
        this.closeConnection = closeConnection;
    }

    @Override
    public void accept(String payload) {
        try {

            Hangup hangupMessage = mapper.readValue(payload, Hangup.class);

            int errorCode = hangupMessage.reasonCode();
            Code code = Code.fromCode(errorCode);

            if (code != null) {
                ErrorHandler.handleResponseErrors(code);
            } else {
                ErrorHandler.handleUnknownErrorCode(errorCode);
            }



            closeConnection.run();
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Failed to process hangup message: " + e.getMessage());
        }
    }


    }

