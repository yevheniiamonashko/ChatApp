package server.handlers;

import shared.utility.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.ClientHandler;
import server.ParseErrorHandler;
import shared.messages.FileTransferReq;
import shared.messages.FileTransferResp;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.function.Consumer;

public class FileTransferReqHandler implements Consumer<String> {

    private final ObjectMapper mapper;
    private final ClientHandler clientHandler;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;
    private final ParseErrorHandler parseErrorHandler;

    public FileTransferReqHandler(ClientHandler clientHandler, ObjectMapper mapper, PrintWriter writer, MessageHandler messageHandler, ParseErrorHandler parseErrorHandler) {
        this.clientHandler = clientHandler;
        this.mapper = mapper;
        this.writer = writer;
        this.messageHandler = messageHandler;
        this.parseErrorHandler = parseErrorHandler;
    }

    @Override
    public void accept(String payload) {
        try {
            FileTransferReq fileTransferReq = mapper.readValue(payload, FileTransferReq.class);
            String receiver = fileTransferReq.receiverOrSender();
            String filename = fileTransferReq.filename();
            String checksum = fileTransferReq.checksum();
            long fileSize = fileTransferReq.fileSize();
            String sender = clientHandler.getUsername();

            if (!clientHandler.checkIfClientExists(sender)) {
                sendFileTransferErrorResponse(Code.UNAUTHORIZED);
                return;
            }


            if (!clientHandler.checkIfClientExists(receiver)) {
                sendFileTransferErrorResponse(Code.NOT_FOUND);
                return;

            }

            clientHandler.sendFileTransferRequestToReceiver(receiver, filename, fileSize, checksum);

        } catch (JsonProcessingException e) {
            parseErrorHandler.handleParseError();
        }

    }

    private void sendFileTransferErrorResponse(Code code) {
        try {
            FileTransferResp fileTransferResp = FileTransferResp.errorWithCode(code.getCode());
            String serializedFileTransferResponseMessage = mapper.writeValueAsString(fileTransferResp);
            Message fileTransferResponseMessage = new Message(Command.FILE_TRANSFER_RESP.getCommand(), serializedFileTransferResponseMessage);
            String serializedMessage = messageHandler.serialize(fileTransferResponseMessage);
            writer.println(serializedMessage);

        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }
}
