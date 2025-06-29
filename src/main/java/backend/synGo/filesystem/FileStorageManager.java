package backend.synGo.filesystem;

import backend.synGo.filesystem.exception.FileControlException;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageManager {
    /**
     * 파일을 업로드하고, 저장된 파일의 경로를 반환합니다.
     *
     * @param multipartFile 업로드할 파일
     * @param nameAndPathToSave 디렉터리를 포함한 파일이 저장될 이름
     * @param originalFileName 원본 파일 이름
     * @return 저장된 파일로 접근할 수 있는 URL
     * @throws FileControlException 파일 업로드 중 오류가 발생한 경우
     */
    String upload(final MultipartFile multipartFile, final String nameAndPathToSave, final String originalFileName) throws FileControlException;

    /**
     * 파일을 업로드하고, 저장된 파일의 경로를 반환합니다.
     *
     * @param content 업로드할 파일의 바이트 정보
     * @param nameAndPathToSave 디렉터리를 포함한 파일이 저장될 이름
     * @param originalFileName 원본 파일 이름
     * @return 저장된 파일로 접근할 수 있는 URL
     * @throws FileControlException 파일 업로드 중 오류가 발생한 경우
     */
    String upload(final byte[] content, final String nameAndPathToSave, final String originalFileName) throws FileControlException;

    /**
     * 파일 삭제 요청
     *
     * @param accessUrl 삭제할 파일의 접근 URL
     */
    void delete(final String accessUrl) throws FileControlException;
}