package client.handlers;

import java.util.function.Consumer;

public class UnknownCommandHandler implements Consumer<String> {

    @Override
    public void accept(String payload) {
        System.out.println("Unknown command was sent to the server. Please send correct request");
    }
}
