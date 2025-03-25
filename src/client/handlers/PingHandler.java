package client.handlers;

import shared.utility.Command;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class PingHandler implements Consumer<String> {

    private  final PrintWriter serverWriter;

    public PingHandler(PrintWriter serverWriter) {
        this.serverWriter = serverWriter;
    }

    @Override
    public void accept(String s) {
        serverWriter.println(Command.PONG.getCommand());

    }
}
