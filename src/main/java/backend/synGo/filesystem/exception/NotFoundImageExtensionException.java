package backend.synGo.filesystem.exception;

public class NotFoundImageExtensionException extends RuntimeException{
    public NotFoundImageExtensionException() {
    }

    public NotFoundImageExtensionException(String message) {
        super(message+ " - 이미지를 찾을 수 없습니다.");
    }

    public NotFoundImageExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundImageExtensionException(Throwable cause) {
        super(cause);
    }

    public NotFoundImageExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
