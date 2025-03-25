package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileTransferAcceptResp(String status, Integer errorCode) {
    public static FileTransferAcceptResp success() {
        return new FileTransferAcceptResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static FileTransferAcceptResp error(int code) {
        return new FileTransferAcceptResp(Status.ERROR.getStatus(), code);
    }
}
