package client.handlers;

import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastResp;

import java.io.IOException;
import java.util.function.Consumer;

public class BroadcastRespHandler implements Consumer<String> {
   private final  ObjectMapper mapper;

    public BroadcastRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void accept(String payload) {
        try {
            // Deserialize the payload into a BroadcastResp object
            BroadcastResp broadcastResp = mapper.readValue(payload, BroadcastResp.class);

            // Retrieve the status and handle accordingly
            Status status = Status.valueOf(broadcastResp.status().toUpperCase());

            switch (status) {
                case OK:
                    break;

                case ERROR:
                    int errorCode = broadcastResp.code();
                    Code code = Code.fromCode(errorCode);

                    if (code != null) {
                        ErrorHandler.handleResponseErrors(code); // Use the utility method
                    } else {
                        ErrorHandler.handleUnknownErrorCode(errorCode); // Handle unknown codes
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status in response: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to process broadcast response: " + e.getMessage());
        }
    }
}
