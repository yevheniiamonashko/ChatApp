package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.utils.Utils;
import shared.messages.Enter;
import shared.messages.UserListResp;
import shared.utility.Command;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.*;

public class SingleUserListTests {
    private final static Properties PROPS = new Properties();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final static int MAX_DELTA_ALLOWED_MS = 500;
    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = SingleUserListTests.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }
    @BeforeEach
    void setup() throws IOException {

        socket = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    @AfterEach
    void cleanup() throws IOException {
        socket.close();
    }
    @Test
    void UserListRequestWhenNotLoggedInReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(in); // Ready message

        // Request user list without entering
        out.println(Command.USER_LIST_REQ.getCommand());
        out.flush();
        String userListResponse = receiveLineWithTimeout(in);
        UserListResp userListResp = Utils.messageToObject(userListResponse);
        assertEquals("ERROR", userListResp.status());
        assertEquals(6000, userListResp.code());
    }

    @Test
    void UserListRequestReturnsEmptyListWhenOnlyOneUserIsLoggedIn() throws JsonProcessingException {
        receiveLineWithTimeout(in); // Ready message

        // Log in as user1
        out.println(Utils.objectToMessage(new Enter("user1")));
        out.flush();
        receiveLineWithTimeout(in); // OK response

        // Request user list
        out.println(Command.USER_LIST_REQ.getCommand());
        out.flush();
        String userListResponse = receiveLineWithTimeout(in);
        UserListResp userListResp = Utils.messageToObject(userListResponse);

        // Verify response
        assertEquals("OK", userListResp.status());
        assertEquals(0, userListResp.users().size()); // User list should be empty
    }




    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }




}
