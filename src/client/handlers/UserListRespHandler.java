package client.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.EnterResp;
import shared.messages.UserListResp;
import shared.utility.Code;
import shared.utility.ErrorHandler;
import shared.utility.Status;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class UserListRespHandler implements Consumer<String> {

    private final ObjectMapper mapper;

    public UserListRespHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public void accept(String payload) {
        try {

            UserListResp userListResp = mapper.readValue(payload, UserListResp.class);


            Status status = Status.valueOf(userListResp.status().toUpperCase());

            switch (status) {
                case OK:
                    printUserList(userListResp.users());
                    break;


                case ERROR:
                    int errorCode = userListResp.code();
                    Code code = Code.fromCode(errorCode);

                    if (code != null) {
                        ErrorHandler.handleResponseErrors(code);
                    } else {
                        ErrorHandler.handleUnknownErrorCode(errorCode);
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid status in response: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to process user list response: " + e.getMessage());
        }
    }


    private void printUserList(List<String> users) {
        if ( users.isEmpty()) {
            System.out.println("There is currently no other logged in users on server to display.");
        } else {
            System.out.print("List of logged in users: ");
            System.out.println(String.join(", ", users));
        }
    }

}



