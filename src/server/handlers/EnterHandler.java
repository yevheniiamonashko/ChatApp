package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.Enter;
import shared.messages.EnterResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class EnterHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;


    public EnterHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;


    }

    @Override
    public void accept(String payload) {
        try {
            Enter enterMessage = mapper.readValue(payload, Enter.class);
            String username = enterMessage.username();

            if (clientHandler.getUsername() != null) {
                sendLoginErrorMessage(Code.USER_ALREADY_LOGGED_IN);
                return;

            }

            if (!isValidUsername(username)) {

                sendLoginErrorMessage(Code.INVALID_USERNAME_LENGTH_OR_FORMAT);
                return;

            }

            if (clientHandler.checkIfClientExists(username)) {
                sendLoginErrorMessage(Code.USER_ALREADY_EXISTS);
                return;

            }


            clientHandler.loginUser(username);


        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }

    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[A-Za-z0-9_]{3,14}$");
    }

    private void sendLoginErrorMessage(Code code) {
        try {
            EnterResp errorResponsePayLoad = EnterResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponsePayLoad);
            Message loginErrorMessage = new Message(Command.ENTER_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(loginErrorMessage);
            writer.println(serializedMessage);
            System.out.println(serializedMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}