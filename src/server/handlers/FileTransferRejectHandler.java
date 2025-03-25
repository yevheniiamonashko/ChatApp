package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.FileTransferReject;
import shared.messages.FileTransferRejectResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class FileTransferRejectHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;


    public FileTransferRejectHandler(ObjectMapper mapper, ClientHandler clientHandler, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.mapper = mapper;
        this.clientHandler = clientHandler;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;


    }
    @Override
    public void accept(String payload) {
        try {
            FileTransferReject fileTransferReject = mapper.readValue(payload, FileTransferReject.class);
            String sender = fileTransferReject.fileTransferInitiator();
            if (!clientHandler.checkIfClientExists(sender)) {
                sendFileRejectErrorMessage(Code.NOT_FOUND);
                return;

            }
            clientHandler.handleSuccessfulFileTransferReject(sender);

        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }

    }

    private void sendFileRejectErrorMessage(Code code) {
        try {
            FileTransferRejectResp errorResponse = FileTransferRejectResp.error(code.getCode());
            String serializedPayload = mapper.writeValueAsString(errorResponse);
            Message fileTransferRejectErrormessage = new Message(Command.FILE_TRANSFER_REJECT_RESP.getCommand(), serializedPayload);
            String serializedMessage = messageHandler.serialize(fileTransferRejectErrormessage);
            writer.println(serializedMessage);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing file transfer reject error message: " + e.getMessage());
        }
    }
}
