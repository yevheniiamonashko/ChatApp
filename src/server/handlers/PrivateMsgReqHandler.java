package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.PrivateMessageReq;
import shared.messages.PrivateMessageResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class PrivateMsgReqHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;

    public PrivateMsgReqHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;

    }

    @Override
    public void accept(String payload) {
        try {
            PrivateMessageReq privateMsgReqMessage = mapper.readValue(payload, PrivateMessageReq.class);
            String message = privateMsgReqMessage.message();
            String sender = clientHandler.getUsername();
            String recipient = privateMsgReqMessage.recipient();


            if (!clientHandler.checkIfClientExists(sender)) {
                sendPrivateMsgReqErrorMessage(Code.UNAUTHORIZED);
                return;
            }


            if (!clientHandler.checkIfClientExists(recipient)) {
                sendPrivateMsgReqErrorMessage(Code.NOT_FOUND);
                return;

            }
            clientHandler.sendPrivateMessage(this.clientHandler.getUsername(), recipient, message);


        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }
    }


    private void sendPrivateMsgReqErrorMessage(Code code) {
        try {
            PrivateMessageResp errorResponsePayLoad = PrivateMessageResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponsePayLoad);
            Message privateMsgResponseErrorMessage = new Message(Command.PRIVATE_MSG_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(privateMsgResponseErrorMessage);
            writer.println(serializedMessage);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}

