package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import shared.messages.UserListResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class UserListReqHandler implements Consumer<String> {

    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;


    public UserListReqHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler) {
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {


        String username = clientHandler.getUsername();



        if (!clientHandler.checkIfClientExists(username)) {
            sendErrorResponse(Code.UNAUTHORIZED);
            return;
        }



        clientHandler.sendListOfUsersToUser();


    }


    private void sendErrorResponse(Code code) {
        try {
            System.out.println("Sending error response");
            // Create the error payload
            UserListResp errorResponsePayload = UserListResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponsePayload);

            // Create the error message
            Message userListResponseErrorMessage = new Message(Command.USER_LIST_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(userListResponseErrorMessage);

            // Send the error message to the client
            writer.println(serializedMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}




