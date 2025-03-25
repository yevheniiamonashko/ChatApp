package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Left;

import java.io.IOException;
import java.util.function.Consumer;

public class LeftHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public LeftHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void accept(String payload) {
        try {

            Left left = mapper.readValue(payload, Left.class);

            // Print the message
            System.out.println("User left: " + left.username());

        } catch (IOException e) {
            System.err.println("Failed to process left message: " + e.getMessage());
        }

    }
}
