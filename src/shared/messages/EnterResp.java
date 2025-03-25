package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON
public record EnterResp(String status, Integer code) {
    // Factory method for a successful response
    public static EnterResp success() {
        return new EnterResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static EnterResp error(int code) {
        return new EnterResp(Status.ERROR.getStatus(), code);
    }
}
