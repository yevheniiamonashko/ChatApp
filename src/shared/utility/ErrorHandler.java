package shared.utility;

public class ErrorHandler {
    /**
     * Handles error codes by printing their corresponding descriptions.
     *
     * @param errorCode The error code to handle.
     */
    public static void handleResponseErrors(Code errorCode) {
        if (errorCode != null) {
            System.out.println("Error: " + errorCode.getDescription());
        } else {
            System.out.println("Unknown error code.");
        }
    }

    /**
     * Handles an unknown error code by printing a default message.
     *
     * @param errorCode The unknown error code.
     */
    public static void handleUnknownErrorCode(int errorCode) {
        System.out.println("Unknown error code: " + errorCode);
    }
}
