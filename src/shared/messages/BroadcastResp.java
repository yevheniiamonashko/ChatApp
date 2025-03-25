package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON

public record BroadcastResp(String status, Integer code) {
    public static BroadcastResp success() {
        return new BroadcastResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static BroadcastResp error(int code) {
        return new BroadcastResp(Status.ERROR.getStatus(), code);
    }
}
