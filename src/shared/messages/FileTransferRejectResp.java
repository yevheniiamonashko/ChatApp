package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileTransferRejectResp (String status, Integer errorCode){
    public static FileTransferRejectResp success() {
        return new FileTransferRejectResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static FileTransferRejectResp error(int code) {
        return new FileTransferRejectResp(Status.ERROR.getStatus(), code);
    }
}
