package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Ready;

import java.io.IOException;
import java.util.function.Consumer;

public class ReadyMesHandler implements Consumer<String> {
     private final ObjectMapper mapper;
    private final Runnable closeRunnable;

    public ReadyMesHandler( ObjectMapper mapper, Runnable closeRunnable ) {
        this.mapper = mapper;
       this.closeRunnable = closeRunnable;
    }
    @Override
    public void accept(String payload) {
        try {



            Ready readyMessage = mapper.readValue(payload, Ready.class);


            System.out.println("Successfully connected to server, version: " + readyMessage.version());

        } catch (IOException e) {

            System.err.println("Failed to process READY message: " + e.getMessage());
            closeRunnable.run();
            System.exit(1);
        }
    }
}
