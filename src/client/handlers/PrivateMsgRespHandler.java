package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.EnterResp;
import shared.messages.PrivateMessageResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.io.IOException;
import java.util.function.Consumer;

public class PrivateMsgRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public PrivateMsgRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void accept(String payload) {
        try {

            PrivateMessageResp privateMsgResp = mapper.readValue(payload, PrivateMessageResp.class);


            Status status = Status.valueOf(privateMsgResp.status().toUpperCase());

            switch (status) {
                case OK:
                    System.out.println("Message sent successfully");
                    break;


                case ERROR:
                    int errorCode = privateMsgResp.code();
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
            System.err.println("Failed to process private message response: " + e.getMessage());
        }
    }

    }

