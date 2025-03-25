package protocoltests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocoltests.protocol.utils.Utils;
import shared.messages.*;
import shared.utility.Command;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class GameTests {

    private final static Properties PROPS = new Properties();

    private Socket socketPlayer1, socketPlayer2;
    private BufferedReader inPlayer1, inPlayer2;
    private PrintWriter outPlayer1, outPlayer2;

    private final static int MAX_DELTA_ALLOWED_MS = 500;

    @BeforeAll
    static void setupAll() throws IOException {
        InputStream in = GameTests.class.getResourceAsStream("testconfig.properties");
        PROPS.load(in);
        in.close();
    }

    @BeforeEach
    void setup() throws IOException, InterruptedException {

        socketPlayer1 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inPlayer1 = new BufferedReader(new InputStreamReader(socketPlayer1.getInputStream()));
        outPlayer1 = new PrintWriter(socketPlayer1.getOutputStream(), true);

        socketPlayer2 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        inPlayer2 = new BufferedReader(new InputStreamReader(socketPlayer2.getInputStream()));
        outPlayer2 = new PrintWriter(socketPlayer2.getOutputStream(), true);
    }

    @AfterEach
    void cleanup() throws IOException {
        socketPlayer1.close();
        socketPlayer2.close();
    }


    @Test
    void ValidGameRequestAndCorrectMoveSubmissionsReturnsCorrectGameResult() throws JsonProcessingException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notification
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());

        // player 2 submits a move
        outPlayer2.println(Utils.objectToMessage(new GameMove("P")));
        outPlayer2.flush();
        String moveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp moveResponse2 = Utils.messageToObject(moveResp2);
        assertEquals("OK", moveResponse2.status());

        // Both players receive the game result
        String result1 = receiveLineWithTimeout(inPlayer1);
        GameResult gameResult1 = Utils.messageToObject(result1);
        assertEquals("player2", gameResult1.winner());
        assertEquals("R", gameResult1.initiatorMove());
        assertEquals("P", gameResult1.opponentMove());

        String result2 = receiveLineWithTimeout(inPlayer2);
        GameResult gameResult2 = Utils.messageToObject(result2);
        assertEquals("player2", gameResult2.winner());
        assertEquals("R", gameResult2.initiatorMove());
        assertEquals("P", gameResult2.opponentMove());
    }

    @Test
    void GameRequestSentByNotLoggedInUserReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inPlayer1); // Ready message

        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String response = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResp = Utils.messageToObject(response);
        assertEquals("ERROR", gameStartResp.status());
        assertEquals(6000, gameStartResp.code());
    }
    @Test
    void GameRequestSentByLoggedInUserButWithNonExistentOpponentReturnsError() throws JsonProcessingException {
        receiveLineWithTimeout(inPlayer1); // Ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // OK response

        outPlayer1.println(Utils.objectToMessage(new GameStartReq("nonExistent")));
        outPlayer1.flush();
        String response = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResp = Utils.messageToObject(response);
        assertEquals("ERROR", gameStartResp.status());
        assertEquals(6004, gameStartResp.code());
    }

    @Test
    void GameRequestWhenAGameOnServerAlreadyRunningReturnsErrorResponse() throws JsonProcessingException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notification
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());


        outPlayer2.println(Utils.objectToMessage(new GameStartReq("player1")));
        outPlayer2.flush();
        String invalidGameRequestResponse = receiveLineWithTimeout(inPlayer2);
        GameStartResp invalidGameResp = Utils.messageToObject(invalidGameRequestResponse);
        assertEquals("ERROR", invalidGameResp.status());
        assertEquals(9000, invalidGameResp.code());


        // player 2 submits a move
        outPlayer2.println(Utils.objectToMessage(new GameMove("P")));
        outPlayer2.flush();
        String moveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp moveResponse2 = Utils.messageToObject(moveResp2);
        assertEquals("OK", moveResponse2.status());

        // Both players receive the game result
        String result1 = receiveLineWithTimeout(inPlayer1);
        GameResult gameResult1 = Utils.messageToObject(result1);
        assertEquals("player2", gameResult1.winner());
        assertEquals("R", gameResult1.initiatorMove());
        assertEquals("P", gameResult1.opponentMove());

        String result2 = receiveLineWithTimeout(inPlayer2);
        GameResult gameResult2 = Utils.messageToObject(result2);
        assertEquals("player2", gameResult2.winner());
        assertEquals("R", gameResult2.initiatorMove());
        assertEquals("P", gameResult2.opponentMove());
    }

    @Test
    void SubmissionOfInvalidMoveInActiveGameByParticipantReturnsErrorResponse() throws JsonProcessingException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notification
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());




        // player 2 submits an invalid move
        outPlayer2.println(Utils.objectToMessage(new GameMove(null)));
        outPlayer2.flush();
        String moveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp moveResponse2 = Utils.messageToObject(moveResp2);
        assertEquals("ERROR", moveResponse2.status());
        assertEquals(9001, moveResponse2.code());

        // player 2 submits a move
        outPlayer2.println(Utils.objectToMessage(new GameMove("P")));
        outPlayer2.flush();
        String validMoveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp validMoveResponse2 = Utils.messageToObject(validMoveResp2);
        assertEquals("OK", validMoveResponse2.status());



        // Both players receive the game result
        String result1 = receiveLineWithTimeout(inPlayer1);
        GameResult gameResult1 = Utils.messageToObject(result1);
        assertEquals("player2", gameResult1.winner());
        assertEquals("R", gameResult1.initiatorMove());
        assertEquals("P", gameResult1.opponentMove());

        String result2 = receiveLineWithTimeout(inPlayer2);
        GameResult gameResult2 = Utils.messageToObject(result2);
        assertEquals("player2", gameResult2.winner());
        assertEquals("R", gameResult2.initiatorMove());
        assertEquals("P", gameResult2.opponentMove());
    }

    @Test
    void RepeatedSubmissionOfValidMoveInActiveGameByParticipantReturnsErrorResponse() throws JsonProcessingException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notification
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());




        // player 1 submits a move again
        outPlayer1.println(Utils.objectToMessage(new GameMove("S")));
        outPlayer1.flush();
        String repeatedMoveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp repeatedMoveResponse1 = Utils.messageToObject(repeatedMoveResp1);
        assertEquals("ERROR", repeatedMoveResponse1.status());
        assertEquals(9004, repeatedMoveResponse1.code());

        // player 2 submits a move
        outPlayer2.println(Utils.objectToMessage(new GameMove("P")));
        outPlayer2.flush();
        String validMoveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp validMoveResponse2 = Utils.messageToObject(validMoveResp2);
        assertEquals("OK", validMoveResponse2.status());



        // Both players receive the game result
        String result1 = receiveLineWithTimeout(inPlayer1);
        GameResult gameResult1 = Utils.messageToObject(result1);
        assertEquals("player2", gameResult1.winner());
        assertEquals("R", gameResult1.initiatorMove());
        assertEquals("P", gameResult1.opponentMove());

        String result2 = receiveLineWithTimeout(inPlayer2);
        GameResult gameResult2 = Utils.messageToObject(result2);
        assertEquals("player2", gameResult2.winner());
        assertEquals("R", gameResult2.initiatorMove());
        assertEquals("P", gameResult2.opponentMove());
    }

    @Test
    void MoveSubmissionByNonParticipantReturnsErrorResponse() throws JsonProcessingException, IOException {
        // login all three players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        Socket socketPlayer3 = new Socket(PROPS.getProperty("host"), Integer.parseInt(PROPS.getProperty("port")));
        BufferedReader inPlayer3 = new BufferedReader(new InputStreamReader(socketPlayer3.getInputStream()));
        PrintWriter outPlayer3 = new PrintWriter(socketPlayer3.getOutputStream(), true);
        receiveLineWithTimeout(inPlayer3); // ready message
        outPlayer3.println(Utils.objectToMessage(new Enter("player3")));
        outPlayer3.flush();
        receiveLineWithTimeout(inPlayer3); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing
        receiveLineWithTimeout(inPlayer2); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notification
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 3 (non-participant) submits a move
        outPlayer3.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer3.flush();
        String moveResp3 = receiveLineWithTimeout(inPlayer3);
        GameMoveResp moveResponse3 = Utils.messageToObject(moveResp3);
        assertEquals("ERROR", moveResponse3.status());
        assertEquals(9003, moveResponse3.code());

        // Cleanup for player 3
        socketPlayer3.close();

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());

        // player 2 submits a move
        outPlayer2.println(Utils.objectToMessage(new GameMove("P")));
        outPlayer2.flush();
        String validMoveResp2 = receiveLineWithTimeout(inPlayer2);
        GameMoveResp validMoveResponse2 = Utils.messageToObject(validMoveResp2);
        assertEquals("OK", validMoveResponse2.status());

        // Both players receive the game result
        String result1 = receiveLineWithTimeout(inPlayer1);
        GameResult gameResult1 = Utils.messageToObject(result1);
        assertEquals("player2", gameResult1.winner());
        assertEquals("R", gameResult1.initiatorMove());
        assertEquals("P", gameResult1.opponentMove());

        String result2 = receiveLineWithTimeout(inPlayer2);
        GameResult gameResult2 = Utils.messageToObject(result2);
        assertEquals("player2", gameResult2.winner());
        assertEquals("R", gameResult2.initiatorMove());
        assertEquals("P", gameResult2.opponentMove());

    }

    @Test
    void MoveSubmissionByLoggedInUserAndNoInstantiatedGameOnServerReturnsError() throws JsonProcessingException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response



        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("ERROR", moveResponse1.status());
        assertEquals(9002, moveResponse1.code());


    }


    @Test
    void GameCancellationWhenPlayerDisconnects() throws JsonProcessingException, IOException {
        // login both players
        receiveLineWithTimeout(inPlayer1); // ready message
        outPlayer1.println(Utils.objectToMessage(new Enter("player1")));
        outPlayer1.flush();
        receiveLineWithTimeout(inPlayer1); // enter response

        receiveLineWithTimeout(inPlayer2); // ready message
        outPlayer2.println(Utils.objectToMessage(new Enter("player2")));
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2); // enter response
        receiveLineWithTimeout(inPlayer1); // joined message processing

        // player 1 initiates the game
        outPlayer1.println(Utils.objectToMessage(new GameStartReq("player2")));
        outPlayer1.flush();
        String gameStartResp = receiveLineWithTimeout(inPlayer1);
        GameStartResp gameStartResponse = Utils.messageToObject(gameStartResp);
        assertEquals("OK", gameStartResponse.status());

        // player 2 receives game invitation
        String gameInvitation = receiveLineWithTimeout(inPlayer2);
        GameInvite gameInvite = Utils.messageToObject(gameInvitation);
        assertEquals("player1", gameInvite.initiator());

        // player 1 and 2 receive game notifications
        String gameNotification1 = receiveLineWithTimeout(inPlayer1);
        GameNotification notification1 = Utils.messageToObject(gameNotification1);
        assertEquals("player1", notification1.initiator());
        assertEquals("player2", notification1.opponent());

        String gameNotification2 = receiveLineWithTimeout(inPlayer2);
        GameNotification notification2 = Utils.messageToObject(gameNotification2);
        assertEquals("player1", notification2.initiator());
        assertEquals("player2", notification2.opponent());

        // player 1 submits a move
        outPlayer1.println(Utils.objectToMessage(new GameMove("R")));
        outPlayer1.flush();
        String moveResp1 = receiveLineWithTimeout(inPlayer1);
        GameMoveResp moveResponse1 = Utils.messageToObject(moveResp1);
        assertEquals("OK", moveResponse1.status());



        outPlayer2.println(Command.BYE.getCommand());
        outPlayer2.flush();
        receiveLineWithTimeout(inPlayer2);

        // Player 1 processes the "LEFT" message
        String leftMessage = receiveLineWithTimeout(inPlayer1);
        Left leftNotification = Utils.messageToObject(leftMessage);
        assertEquals("player2", leftNotification.username());



        // Player 1 receives game cancellation
        String gameCancellation = receiveLineWithTimeout(inPlayer1);
        GameCancelled cancellation = Utils.messageToObject(gameCancellation);
        assertEquals(9005, cancellation.errorCode());
    }





    private String receiveLineWithTimeout(BufferedReader reader) {
        return assertTimeoutPreemptively(ofMillis(MAX_DELTA_ALLOWED_MS), reader::readLine);
    }




}
