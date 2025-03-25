package shared.utility.model;


public class Message {
    private final  String header;
    private  final String payload;



    public Message(String command, String payload) {
        this.header = command;
        this.payload = payload;


    }
    public String getHeader() {
        return header;
    }
    public String getPayload() {
        return payload;
    }





}
