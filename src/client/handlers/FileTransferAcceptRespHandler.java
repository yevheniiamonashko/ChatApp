package client.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.FileTransferAcceptResp;
import shared.messages.FileTransferResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.util.function.Consumer;

public class FileTransferAcceptRespHandler implements Consumer<String> {
    private final ObjectMapper mapper;

    public FileTransferAcceptRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public void accept(String payload) {
        try {
            FileTransferAcceptResp fileTransferAcceptResp = mapper.readValue(payload, FileTransferAcceptResp.class);
            Status status = Status.valueOf(fileTransferAcceptResp.status().toUpperCase());

            if (status == Status.OK) {
                System.out.println("Acceptance sent successfully. File transfer will start.");

            } else if (status == Status.ERROR) {

                int errorCode = fileTransferAcceptResp.errorCode();
                Code code = Code.fromCode(errorCode);
                if (code != null) {
                    System.out.println("Your file transfer acceptance failed due to reason:" + code.getDescription());
                } else {
                    ErrorHandler.handleUnknownErrorCode(errorCode);
                }


            }


        } catch (JsonProcessingException e) {
            System.out.println("Error processing file transfer acceptance response: " + e.getMessage());
        }

    }
}
