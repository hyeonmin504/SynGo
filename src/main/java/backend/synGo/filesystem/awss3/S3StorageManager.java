package backend.synGo.filesystem.awss3;

import backend.synGo.filesystem.FileStorageManager;
import backend.synGo.filesystem.exception.FileControlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageManager implements FileStorageManager {

    @Value("${aws.s3.bucket-name}")
    private String bucket;

    @Value("${aws.s3.asset-root-directory}")
    private String assetRootDirectory;

    @Value("${aws.cloud-front.domain}")
    private String cloudFrontBaseDomain;

    private final S3Client s3Client;
    private final CloudfrontCacheInvalidator cloudfrontCacheInvalidator;

    /**
     * MultipartFile 타입의 파일을 S3에 업로드하는 메서드
     * @param multipartFile 업로드할 파일
     * @param nameAndPathToSave S3에 저장할 경로 및 파일명 (디렉토리 포함)
     * @param originalFileName 원래 파일 이름 (확장자 판단용)
     * @return 업로드된 파일의 CloudFront 기반 public URL
     */
    @Override
    public String upload(final MultipartFile multipartFile, final String nameAndPathToSave, final String originalFileName) {
        try {
            // 요청 바디 생성: InputStream 기반으로 S3 업로드 준비
            final RequestBody requestBody = RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize());

            // 파일 MIME 타입 자동 추정 (ex: image/jpeg)
            final MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(Paths.get(originalFileName)));

            return uploadFile(nameAndPathToSave, requestBody, mediaType);
        } catch (IOException e) {
            log.error("MultipartFile - s3 업로드 예외", e);
            throw new FileControlException("MultipartFile - s3 업로드 예외", e);
        }
    }

    /**
     * byte 배열 기반으로 S3에 업로드하는 메서드
     * @param content 바이트 배열
     * @param nameAndPathToSave 저장 경로 및 파일명
     * @param originalFileName 원본 파일 이름 (확장자 추정용)
     * @return CloudFront URL
     */
    @Override
    public String upload(byte[] content, String nameAndPathToSave, String originalFileName) throws FileControlException {
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            final RequestBody requestBody = RequestBody.fromInputStream(inputStream, content.length);
            final MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(Paths.get(originalFileName)));

            return uploadFile(nameAndPathToSave, requestBody, mediaType);
        } catch (IOException e) {
            log.error("byte array s3 업로드 예외", e);
            throw new FileControlException("byte array s3 업로드 예외", e);
        }
    }

    /**
     * 업로드 공통 로직을 담당하는 내부 메서드
     * 실제 S3 putObject 호출 + CloudFront 캐시 무효화 포함
     * @param directoryPath S3 내 저장 경로
     * @param requestBody 업로드할 파일 데이터
     * @param mediaType 파일의 MIME 타입
     * @return CloudFront URL
     */
    private String uploadFile(final String directoryPath, final RequestBody requestBody, final MediaType mediaType) {
        // 슬래시 처리 개선
        String cleanAssetRoot = assetRootDirectory.endsWith("/") ?
                assetRootDirectory.substring(0, assetRootDirectory.length() - 1) :
                assetRootDirectory;
        String cleanDirectoryPath = directoryPath.startsWith("/") ?
                directoryPath.substring(1) :
                directoryPath;

        final String uploadPath = cleanAssetRoot + "/" + cleanDirectoryPath;

        log.info("Uploading to S3 - Bucket: {}, Key: {}", bucket, uploadPath);

        // S3 put 요청 생성
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uploadPath)
                .contentType(mediaType.toString())
                .build();

        // S3에 파일 업로드 수행
        s3Client.putObject(putObjectRequest, requestBody);

        // CloudFront 경로 무효화 요청 (CDN 캐시 리프레시) - 주석처리
        //cloudfrontCacheInvalidator.createInvalidation(directoryPath);

        // 업로드된 파일의 접근 가능한 CloudFront URL 반환
        final String cloudFrontUrl = String.format("https://%s/%s",
                cloudFrontBaseDomain.replace("https://", "").replace("http://", ""),
                uploadPath);

        log.info("File uploaded to S3: {}", uploadPath);
        log.info("CloudFront URL (OAC enabled): {}", cloudFrontUrl);

        // OAC 설정으로 인해 CloudFront URL만 사용 가능
        return cloudFrontUrl;
    }

    /**
     * 파일 삭제 메서드
     * CloudFront 접근 URL을 받아 내부 S3 경로로 변환한 후 삭제
     * @param accessUrl 외부 공개 URL (CloudFront 기반)
     */
    @Override
    public void delete(String accessUrl) throws FileControlException {
        try {
            // CloudFront URL → S3 실제 키로 변환
            final String assertActualPath = accessUrl.replaceFirst(cloudFrontBaseDomain, assetRootDirectory);

            final DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(assertActualPath)
                    .build();

            // S3 삭제 요청
            s3Client.deleteObject(deleteRequest);

        } catch (final SdkClientException exception) {
            log.error("s3 삭제 에러 - sdk client side 예외 발생", exception);
            throw new FileControlException("s3 삭제 에러 - sdk client side 예외 발생", exception);
        } catch (AwsServiceException exception) {
            log.error("s3 삭제 에러 - aws service side 예외 발생", exception);
            throw new FileControlException("s3 삭제 에러 - aws service side 예외 발생", exception);
        }
    }
}