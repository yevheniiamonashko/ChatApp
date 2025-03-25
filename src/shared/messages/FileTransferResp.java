package shared.messages;


import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record FileTransferResp(String status, Integer code) {
    // Factory method for a successful response
    public static FileTransferResp success() {
        return new FileTransferResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static FileTransferResp errorWithCode(int code) {
        return new FileTransferResp(Status.ERROR.getStatus(), code);
    }
    public static FileTransferResp error() {
        return new FileTransferResp(Status.ERROR.getStatus(), null);
    }
}
