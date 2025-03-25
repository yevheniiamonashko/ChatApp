package client.handlers;

import client.Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.FileTransferResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.util.function.Consumer;

public class FileTransferRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;
    private final Client client;

    public FileTransferRespHandler(ObjectMapper mapper, Client client) {
        this.mapper = mapper;
        this.client = client;
    }

    @Override
    public void accept(String payload) {
        try {
            FileTransferResp fileTransferResp = mapper.readValue(payload, FileTransferResp.class);
            Status status = Status.valueOf(fileTransferResp.status().toUpperCase());
            if (status == Status.OK) {
                System.out.println("File transfer was accepted by receiver and will start.");
            } else if (status == Status.ERROR) {

                if (fileTransferResp.code() != null) {
                    int errorCode = fileTransferResp.code();
                    Code code = Code.fromCode(errorCode);
                    if (code != null) {
                        System.out.println("File transfer request failed due to reason:" + code.getDescription());
                    } else {
                        ErrorHandler.handleUnknownErrorCode(errorCode);
                    }

                } else {

                    System.out.println("File transfer was rejected by the receiver.");
                    client.clearFilePath();
                }
            }


        } catch (JsonProcessingException e) {
            System.out.println("Error processing file transfer response: " + e.getMessage());
        }
    }
}
