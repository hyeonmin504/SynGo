package backend.synGo.exception;

public class NotFoundDataException extends RuntimeException{
    public NotFoundDataException() {
    }

    public NotFoundDataException(String message) {
        super(message);
    }

    public NotFoundDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundDataException(Throwable cause) {
        super(cause);
    }

    public NotFoundDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
