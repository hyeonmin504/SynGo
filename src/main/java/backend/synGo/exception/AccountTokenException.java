package backend.synGo.exception;

public class AccountTokenException extends RuntimeException{
    public AccountTokenException() {
    }

    public AccountTokenException(String message) {
        super(message);
    }

    public AccountTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountTokenException(Throwable cause) {
        super(cause);
    }

    public AccountTokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
