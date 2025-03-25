package shared.utility;

public enum Command {

    // Connection Commands
    READY("READY"),
    ENTER("ENTER"),
    ENTER_RESP("ENTER_RESP"),

    // Broadcast Commands
    BROADCAST_REQ("BROADCAST_REQ"),
    BROADCAST_RESP("BROADCAST_RESP"),
    BROADCAST("BROADCAST"),

    // User Presence Notifications
    JOINED("JOINED"),
    LEFT("LEFT"),

    // Heartbeat Commands
    PING("PING"),
    PONG("PONG"),
    HANGUP("HANGUP"),
    PONG_ERROR("PONG_ERROR"),

    // Termination Commands
    BYE("BYE"),
    BYE_RESP("BYE_RESP"),

    // Error and Invalid Commands
    UNKNOWN_COMMAND("UNKNOWN_COMMAND"),
    PARSE_ERROR("PARSE_ERROR"),


    //User list commands
    USER_LIST_REQ("USER_LIST_REQ"),
    USER_LIST_RESP("USER_LIST_RESP"),

    //private messages commands
    PRIVATE_MSG_REQ("PRIVATE_MSG_REQ"),
    PRIVATE_MSG("PRIVATE_MSG"),
    PRIVATE_MSG_RESP("PRIVATE_MSG_RESP"),

    // game commands
    GAME_START_REQ("GAME_START_REQ"),
    GAME_START_RESP("GAME_START_RESP"),
    GAME_NOTIFICATION("GAME_NOTIFICATION"),
    GAME_INVITATION("GAME_INVITATION"),
    GAME_MOVE("GAME_MOVE"),
    GAME_MOVE_RESP("GAME_MOVE_RESP"),
    GAME_RESULT("GAME_RESULT"),
    GAME_CANCELLED("GAME_CANCELLED"),

    // file transfer commands
    FILE_TRANSFER_REQ("FILE_TRANSFER_REQ"),
    FILE_TRANSFER_RESP("FILE_TRANSFER_RESP"),
    FILE_TRANSFER_ACCEPT("FILE_TRANSFER_ACCEPT"),
    FILE_TRANSFER_REJECT("FILE_TRANSFER_REJECT"),
    FILE_TRANSFER_ACCEPT_RESP("FILE_TRANSFER_ACCEPT_RESP"),
    FILE_TRANSFER_REJECT_RESP("FILE_TRANSFER_REJECT_RESP"),
    FILE_TRANSFER_INIT("FILE_TRANSFER_INIT");


    private final String command;

    // Constructor
    Command(String command) {
        this.command = command;
    }

    // Getter for the string representation
    public String getCommand() {
        return command;
    }

    // Method to get an enum from a string
    public static Command getCommandFromString(String commandStr) {
        for (Command command : Command.values()) {
            if (command.command.equalsIgnoreCase(commandStr)) {
                return command;
            }
        }
        return null;
    }


}


