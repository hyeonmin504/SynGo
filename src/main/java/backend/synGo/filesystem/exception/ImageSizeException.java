package backend.synGo.filesystem.exception;

public class ImageSizeException extends RuntimeException{


    public ImageSizeException(String message) {
        super(message);
    }

    public ImageSizeException(final long allowedMaxSize,final long inputImageSize) {
        super(String.format(
                "허용된 이미지의 크기를 초과했습니다. - request info { allowedMaxSize: %d, inputImageSize: %d }",
                allowedMaxSize, inputImageSize));
    }

    public ImageSizeException(Throwable cause) {
        super(cause);
    }

    public ImageSizeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
