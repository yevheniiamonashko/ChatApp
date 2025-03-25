package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.GameNotification;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

public class GameNotificationHandler implements Consumer<String> {
    private final ObjectMapper mapper;


    public GameNotificationHandler(ObjectMapper mapper) {
        this.mapper = mapper;

    }

    @Override
    public void accept(String payload) {
        try {
            GameNotification gameNotification = mapper.readValue(payload, GameNotification.class);
            String initiator = gameNotification.initiator();
            String opponent = gameNotification.opponent();
            System.out.println("Game started between " + initiator + " and " + opponent);
            System.out.println("Please use 5 option of menu to make a move.");

        } catch (IOException e) {
            System.out.println("Failed to process game notification message:" + e.getMessage());
        }
    }


}
