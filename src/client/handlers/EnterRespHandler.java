package client.handlers;

import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;
import com.fasterxml.jackson.databind.ObjectMapper;

import shared.messages.EnterResp;

import java.io.IOException;
import java.util.function.Consumer;

public class EnterRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;


    public EnterRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;

    }

    @Override
    public void accept(String payload) {
        try {

            EnterResp enterResp = mapper.readValue(payload, EnterResp.class);


            Status status = Status.valueOf(enterResp.status().toUpperCase());

            switch (status) {
                case OK:
                    System.out.println("Login successful");

                    break;


                case ERROR:
                    int errorCode = enterResp.code();
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
            System.err.println("Failed to process enter response: " + e.getMessage());
        }
    }
}
