package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.utils.Utils;
import shared.messages.Enter;
import shared.messages.PrivateMessage;
import shared.messages.PrivateMessageReq;
import shared.messages.PrivateMessageResp;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class PrivateMessagesTests {
    private final static Properties PROPS = new Properties();
    private Socket socketSender, socketRecipient;
    private BufferedReader inSender, inRecipient;
    private PrintWriter outSender, outRecipient;

    private final static int MAX_DELTA_ALLOWED_MS = 500;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = PrivateMessagesTests.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException {
        // set up for user sender
        socketSender = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inSender = new BufferedReader(new InputStreamReader(socketSender.getInputStream()));
        outSender = new PrintWriter(socketSender.getOutputStream(), true);

        // set up for user recipient
        socketRecipient = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inRecipient = new BufferedReader(new InputStreamReader(socketRecipient.getInputStream()));
        outRecipient = new PrintWriter(socketRecipient.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketSender.close();
        socketRecipient.close();
    }

    @Test
    void CorrectPrivateMessageRequestReceivesOKResponseAndPrivateMessageSentToRequestedUser() throws JsonProcessingException {
        // log in both users
        receiveLineWithTimeout(inSender); // ready message
        outSender.println(Utils.objectToMessage(new Enter("sender")));
        outSender.flush();
        receiveLineWithTimeout(inSender); // enter response

        receiveLineWithTimeout(inRecipient); // ready message
        outRecipient.println(Utils.objectToMessage(new Enter("recipient")));
        outRecipient.flush();
        receiveLineWithTimeout(inRecipient); // enter response
        receiveLineWithTimeout(inSender); // joined message received by sender

        // sender user sends private message
        String messageContent = "Hello!";
        outSender.println(Utils.objectToMessage(new PrivateMessageReq("recipient", messageContent)));
        outSender.flush();

        // verify that sender receives ok response
        String response = receiveLineWithTimeout(inSender);
        PrivateMessageResp privateMessageResp = Utils.messageToObject(response);
        assertEquals("OK", privateMessageResp.status());

        // verify that recipient user receives the private message
        String recipientResponse = receiveLineWithTimeout(inRecipient);
        PrivateMessage privateMessage = Utils.messageToObject(recipientResponse);
        assertEquals("sender", privateMessage.sender());
        assertEquals(messageContent, privateMessage.message());
    }
    @Test
    void PrivateMessageRequestFromNotLoggedInUserReceivesErrorResponse() throws JsonProcessingException {
        receiveLineWithTimeout(inSender); // Ready message


        String messageContent = "Hello!";
        outSender.println(Utils.objectToMessage(new PrivateMessageReq("recipient", messageContent)));
        outSender.flush();


        // verification that sender gets an error response
        String response = receiveLineWithTimeout(inSender);
        PrivateMessageResp privateMessageResp = Utils.messageToObject(response);
        assertEquals("ERROR", privateMessageResp.status());
        assertEquals(6000, privateMessageResp.code());
    }
    @Test
    void PrivateMessageRequestWithNotExistentRecipientReceivesErrorResponse() throws JsonProcessingException {
        // Sender logs in
        receiveLineWithTimeout(inSender); // ready message
        outSender.println(Utils.objectToMessage(new Enter("sender")));
        outSender.flush();
        receiveLineWithTimeout(inSender); // enter response



        String messageContent = "Hello!";
        outSender.println(Utils.objectToMessage(new PrivateMessageReq("nonExistentUser", messageContent)));
        outSender.flush();


        // verification that sender gets an error response
        String response = receiveLineWithTimeout(inSender);
        PrivateMessageResp privateMessageResp = Utils.messageToObject(response);
        assertEquals("ERROR", privateMessageResp.status());
        assertEquals(6004, privateMessageResp.code());
    }




    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }
}
