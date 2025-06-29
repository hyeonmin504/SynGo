package backend.synGo.filesystem.exception;

public class NotAllowedImageExtensionException extends RuntimeException{
    public NotAllowedImageExtensionException() {
    }

    public NotAllowedImageExtensionException(final String imageName) {
        super("허용되지 않는 확장자입니다. - " + imageName);
    }

    public NotAllowedImageExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAllowedImageExtensionException(Throwable cause) {
        super(cause);
    }

    public NotAllowedImageExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
