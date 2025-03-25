package shared.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record UserListResp(String status, List<String> users, Integer code) {

    public static UserListResp success(List<String> users) {
        return new UserListResp(Status.OK.getStatus(), users, null);
    }

    // Factory method for an error response
    public static UserListResp error(int code) {
        return new UserListResp(Status.ERROR.getStatus(), null, code);
    }
}
