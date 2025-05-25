package backend.synGo.exception;

public class ExistUserException extends RuntimeException{
    public ExistUserException() {
    }

    public ExistUserException(String message) {
        super(message);
    }

    public ExistUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExistUserException(Throwable cause) {
        super(cause);
    }

    public ExistUserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
