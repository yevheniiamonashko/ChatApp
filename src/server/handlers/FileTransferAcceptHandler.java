package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.FileTransferAccept;
import shared.messages.FileTransferAcceptResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class FileTransferAcceptHandler implements Consumer<String> {

    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;


    public FileTransferAcceptHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;


    }

    @Override
    public void accept(String payload) {
        try {
            FileTransferAccept fileTransferAccept = mapper.readValue(payload, FileTransferAccept.class);
            String sender = fileTransferAccept.fileTransferInitiator();
            if (!clientHandler.checkIfClientExists(sender)) {
                sendFileAcceptErrorMessage(Code.NOT_FOUND);
                return;

            }
            clientHandler.handleSuccessfulFileTransferAccept(sender);

        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }
    }

    private void sendFileAcceptErrorMessage(Code code) {
        try {
            FileTransferAcceptResp errorResponse = FileTransferAcceptResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponse);
            Message fileTransferAcceptErrormessage = new Message(Command.FILE_TRANSFER_ACCEPT_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferAcceptErrormessage);
            writer.println(serializedMessage);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing file transfer accept error message: " + e.getMessage());
        }
    }
}
