package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PrivateMessageResp(String status, Integer code) {
    // Factory method for a successful response
    public static PrivateMessageResp success() {
        return new PrivateMessageResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static PrivateMessageResp error(int code) {
        return new PrivateMessageResp(Status.ERROR.getStatus(), code);
    }
}
