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

    // 5MB 용량 제한 (바이트 단위)
    private static final int LIMIT_IMAGE_SIZE = 5 * 1024 * 1024;

    /**
     * 업로드된 이미지 파일의 유효성을 검사하는 메서드
     * @param image 업로드된 이미지
     * @throws ImageSizeException 이미지가 너무 큰 경우
     * @throws NotAllowedImageExtensionException 허용되지 않은 확장자인 경우
     * @throws NotFoundImageExtensionException 확장자를 찾을 수 없는 경우
     */
    public void validate(final MultipartFile image) {
        // 1. 이미지 크기 검사 (용량 제한 초과 시 예외 발생)
        if (image.getSize() > LIMIT_IMAGE_SIZE) {
            throw new ImageSizeException(LIMIT_IMAGE_SIZE, (int) image.getSize());
        }

        // 2. 이미지 확장자 검사
        // 허용된 확장자에 없으면 예외 발생
        if (AllowedImageExtension.isNotContain(getFileExtension(image))) {
            throw new NotAllowedImageExtensionException(
                    "지원하지 않는 이미지 확장자입니다. 파일명: " + image.getOriginalFilename()
            );
        }
    }

    /**
     * MultipartFile에서 확장자를 추출하는 보조 메서드
     * 확장자를 찾지 못하면 별도의 예외로 변환하여 던짐
     */
    private String getFileExtension(final MultipartFile file) {
        try {
            // FileUtil → 확장자 추출 유틸 메서드 호출
            return FileUtil.getFileExtension(file);
        } catch (final FileControlException.FileExtensionException e) {
            // 확장자를 못 찾았을 경우 사용자 정의 예외 변환
            throw new NotFoundImageExtensionException(file.getOriginalFilename());
        }
    }
}
