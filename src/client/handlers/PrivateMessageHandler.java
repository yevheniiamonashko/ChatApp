package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Broadcast;
import shared.messages.PrivateMessage;

import java.io.IOException;
import java.util.function.Consumer;

public class PrivateMessageHandler implements Consumer<String> {

    private final ObjectMapper mapper;

    public PrivateMessageHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {
            PrivateMessage privateMessage = mapper.readValue(payload, PrivateMessage.class);
            String username = privateMessage.sender();
            String message = privateMessage.message();
            System.out.println(username + " says to you: " + message);

        } catch (IOException e) {
            System.err.println("Failed to process private message: " + e.getMessage());
        }

    }
}
