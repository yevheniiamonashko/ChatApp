package shared.messages;



public record FileTransferReq(String receiverOrSender, String filename, long fileSize, String checksum) {
}
