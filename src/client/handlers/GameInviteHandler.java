package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.GameInvite;
import java.io.IOException;
import java.util.function.Consumer;

public class GameInviteHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public GameInviteHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {

            GameInvite gameInvite = mapper.readValue(payload, GameInvite.class);
            String initiator = gameInvite.initiator();
            System.out.println("You was invited to Rock/Paper/Scissors game by " + initiator);


        } catch (IOException e) {
            System.err.println("Failed to process game invitation message :" + e.getMessage());
        }

    }
}
