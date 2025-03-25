package shared.messages;


import com.fasterxml.jackson.annotation.JsonInclude;
import shared.utility.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameStartResp(String status, Integer code, String usernameA, String usernameB) {


    public static GameStartResp success() {
        return new GameStartResp(Status.OK.getStatus(), null, null, null);
    }


    public static GameStartResp error(int code) {
        return new GameStartResp(Status.ERROR.getStatus(), code, null, null);
    }


    public static GameStartResp errorWithUsers(int code, String usernameA, String usernameB) {
        return new GameStartResp(Status.ERROR.getStatus(), code, usernameA, usernameB);
    }
}
