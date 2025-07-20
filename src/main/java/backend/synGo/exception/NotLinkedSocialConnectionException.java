package backend.synGo.exception;

public class NotLinkedSocialConnectionException extends RuntimeException{
    public NotLinkedSocialConnectionException() {
    }

    public NotLinkedSocialConnectionException(String message) {
        super(message);
    }

    public NotLinkedSocialConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotLinkedSocialConnectionException(Throwable cause) {
        super(cause);
    }

    public NotLinkedSocialConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
