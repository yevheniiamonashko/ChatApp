package shared.utility;

public enum Status {
    OK("OK"),
    ERROR("ERROR");


    private  final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
