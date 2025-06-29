package backend.synGo.filesystem;

import backend.synGo.filesystem.exception.FileControlException;
import backend.synGo.filesystem.exception.ImageSizeException;
import backend.synGo.filesystem.exception.NotAllowedImageExtensionException;
import backend.synGo.filesystem.exception.NotFoundImageExtensionException;
import backend.synGo.filesystem.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public class ImageValidationService {

    private static final int LIMIT_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    public void validate(final MultipartFile image) {
        if (image.getSize() > LIMIT_IMAGE_SIZE) {
            throw new ImageSizeException(LIMIT_IMAGE_SIZE, (int) image.getSize());
        }
        if (AllowedImageExtension.isNotContain(getFileExtension(image))) {
            throw new NotAllowedImageExtensionException("지원하지 않는 이미지 확장자입니다. 지원되는 확장자: " + image.getOriginalFilename());
        }
    }

    private String getFileExtension(final MultipartFile file) {
        try {
            return FileUtil.getFileExtension(file);
        } catch (final FileControlException.FileExtensionException e) {
            throw new NotFoundImageExtensionException(file.getOriginalFilename());
        }
    }
}
