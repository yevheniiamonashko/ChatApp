package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Broadcast;


import java.io.IOException;
import java.util.function.Consumer;

public class BroadcastHandler implements Consumer<String> {
    private  final ObjectMapper mapper;

    public BroadcastHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public void accept(String payload) {
        try{
        Broadcast broadcastMessage = mapper.readValue(payload, Broadcast.class);
        String username= broadcastMessage.username();
        String message= broadcastMessage.message();
        System.out.println(username+" says "+message);

    }
    catch (IOException e) {
        System.err.println("Failed to process broadcast message: " + e.getMessage());
        }
    }
}
