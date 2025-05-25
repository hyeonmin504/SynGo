package backend.synGo.exception;

public class NotValidException extends RuntimeException{

    public NotValidException() {
    }

    public NotValidException(String message) {
        super(message);
    }

    public NotValidException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotValidException(Throwable cause) {
        super(cause);
    }

    public NotValidException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
