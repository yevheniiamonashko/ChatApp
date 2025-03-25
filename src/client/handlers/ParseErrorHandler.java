package client.handlers;

import java.util.function.Consumer;

public class ParseErrorHandler implements Consumer<String> {
    @Override
    public void accept(String s) {
     System.out.println("Invalid message format.Please try again");
    }
}
