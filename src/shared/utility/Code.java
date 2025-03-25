package shared.utility;

public enum Code {


    USER_ALREADY_EXISTS(5000, "User with this name already exists"),
    INVALID_USERNAME_LENGTH_OR_FORMAT(5001, "Username has an invalid format or length"),
    USER_ALREADY_LOGGED_IN(5002, "User is already logged in"),
    UNAUTHORIZED(6000, "User is not logged in"),
    NO_PONG(7000, "No activity from the user shown"),
    PONG_WITHOUT_PING(8000, "Server registered failure during checking user activity"),
    NOT_FOUND(6004, "User is not found"),
    GAME_ALREADY_RUNNING(9000, "A game is already running between two users"),
    INVALID_GAME_MOVE(9001, "Invalid move (not 'rock', 'paper', or 'scissors')"),
    NO_ACTIVE_GAME(9002, "No active game on server, the move cannot be submitted."),
    USER_NOT_PARTICIPANT(9003, "You are not a participant in the current game"),
    MOVE_ALREADY_MADE(9004, "You have already made your move"),
    OPPONENT_DISCONNECTED(9005, "Opponent disconnected"),
    MOVES_OR_MOVE_NOT_SUBMITTED(9006, "One or both players failed to submit their moves in time.");



    private final int code;
    private final String description;

    Code(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }


    public static Code fromCode(int code) {
        for (Code c : Code.values()) {
            if (c.code == code) {
                return c;
            }
        }
        return null;
    }
}



