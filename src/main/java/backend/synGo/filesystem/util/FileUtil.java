package backend.synGo.filesystem.util;

import backend.synGo.filesystem.exception.FileControlException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

public class FileUtil {

    //순수 유틸리티 클래스임을 명시적으로 표현.
    private FileUtil() {}

    /**
     * 업로드된 MultipartFile에서 확장자를 추출하는 유틸리티 메서드
     * @param file 사용자가 업로드한 파일 (Spring MultipartFile)
     * @return 파일의 확장자 (예: "jpg", "png", "pdf" 등)
     * @throws FileControlException.FileExtensionException 확장자를 찾을 수 없을 경우 예외 발생
     */
    public static String getFileExtension(final MultipartFile file) {
        // 1. 파일의 원래 이름을 가져옴 (예: "image.png", "resume.pdf" 등)
        final String originalFilename = file.getOriginalFilename();

        // 2. 파일명이 null이 아닌 경우에만 처리 (보통 null일 일은 거의 없음)
        if (Objects.nonNull(originalFilename)) {

            // 3. 마지막 '.'의 위치를 찾음 → 확장자 시작 위치
            int dotIndex = originalFilename.lastIndexOf('.');

            // 4. '.'이 처음이 아니고, 마지막 문자도 아닌 경우 (".a", "a." 등은 제외)
            if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
                // 5. 확장자 부분만 잘라서 반환 (예: "png", "pdf")
                return originalFilename.substring(dotIndex + 1);
            }
        }
        // 6. 확장자를 제대로 찾지 못한 경우 사용자 정의 예외를 던짐
        throw new FileControlException.FileExtensionException(
                "파일 확장자를 찾을 수 없습니다. 파일 이름: " + originalFilename
        );
    }
}
