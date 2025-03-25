package protocoltests;


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

public class MultipleUserListTest {
    private final static Properties PROPS = new Properties();

    private Socket socketUser1, socketUser2, socketUser3;
    private BufferedReader inUser1, inUser2, inUser3;
    private PrintWriter outUser1, outUser2, outUser3;

    private final static int MAX_DELTA_ALLOWED_MS = 500;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = MultipleUserListTest.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException{
        // Setup socket and streams for user 1
        socketUser1 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser1 = new BufferedReader(new InputStreamReader(socketUser1.getInputStream()));
        outUser1 = new PrintWriter(socketUser1.getOutputStream(), true);

        // Setup socket and streams for user 2
        socketUser2 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser2 = new BufferedReader(new InputStreamReader(socketUser2.getInputStream()));
        outUser2 = new PrintWriter(socketUser2.getOutputStream(), true);

        // Setup socket and streams for user 3
        socketUser3 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inUser3 = new BufferedReader(new InputStreamReader(socketUser3.getInputStream()));
        outUser3 = new PrintWriter(socketUser3.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        // Close all sockets after test execution
        socketUser1.close();
        socketUser2.close();
        socketUser3.close();
    }

    @Test
    void UserListRequestReturnsListWithTwoUsersWhenThreeUsersAreLoggedIn() throws IOException {
        // Receive ready message for all users
        receiveLineWithTimeout(inUser1);
        receiveLineWithTimeout(inUser2);
        receiveLineWithTimeout(inUser3);

        // Log in as user1
        outUser1.println(Utils.objectToMessage(new Enter("user1")));
        outUser1.flush();
        receiveLineWithTimeout(inUser1); // OK response

        // Log in as user2
        outUser2.println(Utils.objectToMessage(new Enter("user2")));
        outUser2.flush();
        receiveLineWithTimeout(inUser2); // OK response
        receiveLineWithTimeout(inUser1);

        // Log in as user3
        outUser3.println(Utils.objectToMessage(new Enter("user3")));
        outUser3.flush();
        receiveLineWithTimeout(inUser3); // OK response
        receiveLineWithTimeout(inUser1); // consume Joined message for user1
        receiveLineWithTimeout(inUser2); // consume Joined message for user2

        // Request user list from user1
        outUser1.println(Command.USER_LIST_REQ.getCommand());
        outUser1.flush();
        String userListResponse = receiveLineWithTimeout(inUser1);
        UserListResp userListResp = Utils.messageToObject(userListResponse);

        System.out.println(userListResponse);
        // Verify the response
        assertEquals("OK", userListResp.status());
        assertEquals(2, userListResp.users().size(), "The user list should contain two other users.");
        assertTrue(userListResp.users().contains("user2"));
        assertTrue(userListResp.users().contains("user3"));
    }

    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
