package backend.synGo.filesystem.exception;

public class FileControlException extends RuntimeException{
    public FileControlException() {
    }

    public FileControlException(String message) {
        super(message);
    }

    public FileControlException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileControlException(Throwable cause) {
        super(cause);
    }

    public FileControlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static class FileExtensionException extends RuntimeException {
        public FileExtensionException(final String message) {
            super(message);
        }
    }
}
