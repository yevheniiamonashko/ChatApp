package client.handlers;

import shared.utility.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.ByeResp;

import java.io.IOException;
import java.util.function.Consumer;

public class ByeRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final Runnable onClose;

    public ByeRespHandler(ObjectMapper mapper, Runnable onClose) {
        this.mapper = mapper;
        this.onClose = onClose;
    }

    @Override
    public void accept(String payload) {

        try {
            ByeResp byeResp = mapper.readValue(payload, ByeResp.class);

            Status status = Status.valueOf(byeResp.status().toUpperCase());
            if (status == Status.OK) {
                System.out.println("Successfully logged out. Goodbye!");
                onClose.run();
            } else {
                System.err.println("Failed to log out: " + byeResp.status());
            }
        } catch (IOException e) {
            System.err.println("Error processing BYE_RESP: " + e.getMessage());
        }
    }
}
