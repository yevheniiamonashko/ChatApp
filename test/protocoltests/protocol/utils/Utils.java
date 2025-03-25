package protocoltests.protocol.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final static Map<Class<?>, String> objToNameMapping = new HashMap<>();
    static {
        objToNameMapping.put(Enter.class, "ENTER");
        objToNameMapping.put(EnterResp.class, "ENTER_RESP");
        objToNameMapping.put(BroadcastReq.class, "BROADCAST_REQ");
        objToNameMapping.put(BroadcastResp.class, "BROADCAST_RESP");
        objToNameMapping.put(Broadcast.class, "BROADCAST");
        objToNameMapping.put(Joined.class, "JOINED");
        objToNameMapping.put(ParseError.class, "PARSE_ERROR");
        objToNameMapping.put(Pong.class, "PONG");
        objToNameMapping.put(PongError.class, "PONG_ERROR");
        objToNameMapping.put(Ready.class, "READY");
        objToNameMapping.put(Ping.class, "PING");
        objToNameMapping.put(UserListResp.class, "USER_LIST_RESP");
        objToNameMapping.put(PrivateMessageReq.class, "PRIVATE_MSG_REQ");
        objToNameMapping.put(PrivateMessageResp.class, "PRIVATE_MSG_RESP");
        objToNameMapping.put(PrivateMessage.class, "PRIVATE_MSG");
        objToNameMapping.put(GameStartReq.class, "GAME_START_REQ");
        objToNameMapping.put(GameStartResp.class, "GAME_START_RESP");
        objToNameMapping.put(GameNotification.class, "GAME_NOTIFICATION");
        objToNameMapping.put(GameInvite.class, "GAME_INVITATION");
        objToNameMapping.put(GameMove.class, "GAME_MOVE");
        objToNameMapping.put(GameMoveResp.class, "GAME_MOVE_RESP");
        objToNameMapping.put(GameResult.class, "GAME_RESULT");
        objToNameMapping.put(GameCancelled.class, "GAME_CANCELLED");
        objToNameMapping.put(Left.class, "LEFT");
    }

    public static String objectToMessage(Object object) throws JsonProcessingException {
        Class<?> clazz = object.getClass();
        String header = objToNameMapping.get(clazz);
        if (header == null) {
            throw new RuntimeException("Cannot convert this class to a message");
        }
        String body = mapper.writeValueAsString(object);
        return header + " " + body;
    }

    public static <T> T messageToObject(String message) throws JsonProcessingException {
        String[] parts = message.split(" ", 2);
        if (parts.length > 2 || parts.length == 0) {
            throw new RuntimeException("Invalid message");
        }
        String header = parts[0];
        String body = "{}";
        if (parts.length == 2) {
            body = parts[1];
        }
        Class<?> clazz = getClass(header);
        Object obj = mapper.readValue(body, clazz);
        return (T) clazz.cast(obj);
    }

    private static Class<?> getClass(String header) {
        return objToNameMapping.entrySet().stream()
                .filter(e -> e.getValue().equals(header))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find class belonging to header " + header));
    }
}
