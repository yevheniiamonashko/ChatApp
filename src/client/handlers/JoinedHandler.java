package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Joined;

import java.io.IOException;
import java.util.function.Consumer;

public class JoinedHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public JoinedHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void accept(String payload) {
        try {

            Joined joined = mapper.readValue(payload, Joined.class);

            // Print the message
            System.out.println("User joined: " + joined.username());

        } catch (IOException e) {
            System.err.println("Failed to process joined message: " + e.getMessage());
        }
    }

}
