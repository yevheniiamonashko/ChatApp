package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.BroadcastReq;
import shared.messages.BroadcastResp;

import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class BroadcastReqHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;


    public BroadcastReqHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;
        this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {
            BroadcastReq broadcastReqMessage = mapper.readValue(payload, BroadcastReq.class);
            String message = broadcastReqMessage.message();
            String username = clientHandler.getUsername();


            if (!clientHandler.checkIfClientExists(username)) {
                sendBroadcastReqErrorMessage(Code.UNAUTHORIZED);
                return;
            }


            clientHandler.broadcastMessage(username, message);


        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }
    }


    private void sendBroadcastReqErrorMessage(Code code) {
        try {
            BroadcastResp errorResponsePayLoad = BroadcastResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponsePayLoad);
            Message broadcastResponseErrorMessage = new Message(Command.BROADCAST_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(broadcastResponseErrorMessage);
            writer.println(serializedMessage);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }



}
