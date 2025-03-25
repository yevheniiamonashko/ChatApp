package client.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.FileTransferAcceptResp;
import shared.messages.FileTransferRejectResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.util.function.Consumer;

public class FileTransferRejectRespHandler implements Consumer<String> {

    private final ObjectMapper mapper;

    public FileTransferRejectRespHandler(ObjectMapper mapper) {
       this.mapper = mapper;
    }

    @Override
    public void accept(String payload) {
        try {
            FileTransferRejectResp fileTransferRejectResp = mapper.readValue(payload, FileTransferRejectResp.class);
            Status status = Status.valueOf(fileTransferRejectResp.status().toUpperCase());

            if (status == Status.OK) {
                System.out.println("Rejection sent successfully. The initiator of file transfer request will be notified.");

            } else if (status == Status.ERROR) {

                int errorCode = fileTransferRejectResp.errorCode();
                Code code = Code.fromCode(errorCode);
                if (code != null) {
                    System.out.println("Your file transfer rejection failed due to reason:" + code.getDescription());
                } else {
                    ErrorHandler.handleUnknownErrorCode(errorCode);
                }


            }


        } catch (JsonProcessingException e) {
            System.out.println("Error processing file transfer rejection response: " + e.getMessage());
        }


    }
}
