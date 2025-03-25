package shared.utility;

import com.fasterxml.jackson.core.JsonProcessingException;

import shared.utility.model.Message;

public class MessageHandler {





    public String serialize(Message message) throws JsonProcessingException {
        String command = message.getHeader();
        String payload = message.getPayload();

        if (payload != null) {
            return command + " " + payload;
        } else {
            return command;
        }
    }

    public Message deserialize(String rawMessage) throws JsonProcessingException {

        String[] splits = rawMessage.split(" ", 2);


        String header = splits[0]; // The first part is always the header
        String jsonPayload = splits.length > 1 ? splits[1] : null; // The second part (if exists) is the payload

        // Return a new Message object with the header and payload
        return new Message(header, jsonPayload);
    }





}
