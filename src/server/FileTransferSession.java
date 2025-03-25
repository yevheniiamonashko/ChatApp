package server;

import java.io.InputStream;
import java.io.OutputStream;

public class FileTransferSession {
    private InputStream sender;
    private OutputStream receiver;

   public FileTransferSession() {

   }

    public InputStream getSender() {
        return sender;
    }

    public OutputStream getReceiver() {
        return receiver;

    }

    public void setSender(InputStream sender) {
        this.sender = sender;

    }



    public void setReceiver(OutputStream receiver) {
        this.receiver = receiver;

    }


}
