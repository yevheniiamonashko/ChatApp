package server;

import shared.utility.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.Hangup;
import shared.messages.PongError;
import shared.utility.Code;
import shared.utility.Command;
import shared.utility.MessageHandler;

import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;


public class PongManager {

    private static final int PING_INTERVAL = 10_000; // 10 seconds
    private static final int PING_TIMEOUT = 3_000;  // 3 seconds
    private  Timer pingTimer;
    private TimerTask pongTimeoutTask;
    private volatile boolean pongReceived = true;
    private final ClientHandler clientHandler;
    private final ObjectMapper mapper;
    private final PrintWriter writer;
    private final MessageHandler messageHandler;



    public PongManager(ClientHandler clientHandler, ObjectMapper mapper, PrintWriter writer, MessageHandler messageHandler) {
        this.clientHandler = clientHandler;
        this.mapper = mapper;
        this.writer = writer;
        this.messageHandler = messageHandler;


    }

    /**
     * Starts the periodic ping process to monitor client activity with 10-second interval.
     */
    public void startPingProcess() {
        pingTimer = new Timer(true); // isDaemon set to true to perform ping pong mechanism as a background process
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, PING_INTERVAL, PING_INTERVAL);
    }


    /**
     * Sends a PING message to the client and schedules a timeout task to ensure a response.
     */
    private  synchronized void  sendPing() {
        pongReceived = false;

        try {

            Message pingMessage = new Message(Command.PING.getCommand(), null);
            String serializedPing = messageHandler.serialize(pingMessage);
            writer.println(serializedPing);
            System.out.println("Sent PING to client: " + clientHandler.getUsername());

            schedulePongTimeout();

        } catch (Exception e) {
            System.err.println("Error sending PING to client: " + clientHandler.getUsername() );

        }
    }

    /**
     * Schedules a task to handle a timeout if the client does not respond with a PONG within the expected interval.
     */

    private  synchronized void schedulePongTimeout() {
        if (pongTimeoutTask != null) {
            pongTimeoutTask.cancel();
        }

        pongTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                if (!pongReceived) {
                    System.err.println("Client " + clientHandler.getUsername() + " failed to respond to PING. Disconnecting...");
                    sendHangup(Code.NO_PONG.getCode());
                    clientHandler.handleDisconnection();
                }
            }
        };

        new Timer(true).schedule(pongTimeoutTask, PING_TIMEOUT);
    }


    /**
     * Processes a PONG command received from the client.
     * If a timeout task is active, it confirms the PONG and resets the flag.
     * If no timeout task is scheduled, a PONG_ERROR is sent.
     */

    public synchronized void processPongCommand() {
        if (!pongTimeoutTaskScheduled()) {
            // Server did not expect a PONG
            sendPongError();
        } else {
            handlePong();
        }
    }

    /**
     * Checks if a pong timeout task is currently scheduled.
     *
     * @return true if the pong timeout task is scheduled; false otherwise.
     */
    private boolean pongTimeoutTaskScheduled() {
        // Check if a PONG is expected by verifying if the pongTimeoutTask is scheduled
        return pongTimeoutTask != null;
    }


    /**
     * Handles a valid PONG response from the client, resetting the `pongReceived` flag.
     */
    private synchronized void  handlePong() {
        System.out.println("Received PONG from client: " + clientHandler.getUsername());
        pongReceived = true;
    }




    /**
     * Sends a HANGUP message to the client to notify them of disconnection due to timeout.
     *
     * @param reasonCode The reason code for the disconnection.
     */
    private void sendHangup(int reasonCode) {
        try {
            Hangup hangupPayload = new Hangup(reasonCode);
            String payload = mapper.writeValueAsString(hangupPayload);
            Message hangupMessage = new Message(Command.HANGUP.getCommand(), payload);
            String serializedMessage = messageHandler.serialize(hangupMessage);
            writer.println(serializedMessage);
        } catch (Exception e) {
            System.err.println("Error sending HANGUP to client: " + clientHandler.getUsername());
        }
    }

    /**
     * Sends a PONG_ERROR message to the client if they send a PONG without a PING.
     */

    private void sendPongError() {
        try {
            PongError pongError = new PongError(Code.PONG_WITHOUT_PING.getCode());
            String payload = mapper.writeValueAsString(pongError);
            Message pongErrorMessage = new Message(Command.PONG_ERROR.getCommand(), payload);
            String serializedMessage = messageHandler.serialize(pongErrorMessage);
            writer.println(serializedMessage);
            System.out.println("Sent PONG_ERROR to client: " + clientHandler.getUsername());
        } catch (Exception e) {
            System.err.println("Error sending PONG_ERROR to client: " + clientHandler.getUsername());
        }
    }


    /**
     * Stops the ongoing ping process and cancels any active timeout tasks.
     */
    public void stopPingProcess() {
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer.purge();
            pingTimer = null;
        }
        if (pongTimeoutTask != null) {
            pongTimeoutTask.cancel();
            pongTimeoutTask = null;
        }
        System.out.println("Stopped ping process for client: " + clientHandler.getUsername());
    }


}
