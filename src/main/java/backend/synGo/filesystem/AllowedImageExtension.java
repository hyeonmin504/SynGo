package backend.synGo.filesystem;

import java.util.Arrays;

public enum AllowedImageExtension {
    JPEG, PNG, GIF, WEBP;
    /**
     * 주어진 확장자가 허용된 이미지 확장자 목록에 포함되지 않았는지 검사
     * @param imageExtension 사용자가 업로드한 이미지 파일의 확장자 (예: "jpg", "png" 등)
     * @return 포함되지 않으면 true (즉, 허용되지 않은 확장자면 true)
     */
    public static boolean isNotContain(final String imageExtension) {
        // 1. 대소문자 구분 없이 비교하기 위해 입력값을 대문자로 변환
        final String upperCase = imageExtension.toUpperCase();

        // 2. enum 상수 전체를 순회하면서 동일한 이름이 있는지 비교
        //    하나도 일치하지 않으면 noneMatch → true 반환
        return Arrays.stream(values())
                .noneMatch(allowedImageExtension ->
                        allowedImageExtension.name().equals(upperCase)
                );
    }
}