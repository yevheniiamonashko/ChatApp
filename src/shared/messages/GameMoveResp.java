package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record GameMoveResp(String status, Integer code) {
    public static GameMoveResp success() {
        return new GameMoveResp(Status.OK.getStatus(), null);
    }

    // Factory method for an error response
    public static GameMoveResp error(int code) {
        return new GameMoveResp(Status.ERROR.getStatus(), code);
    }
}
