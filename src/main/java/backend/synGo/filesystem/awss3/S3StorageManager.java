package backend.synGo.filesystem.awss3;

import backend.synGo.filesystem.FileStorageManager;
import backend.synGo.filesystem.exception.FileControlException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

    @Override
    public String upload(final MultipartFile multipartFile,final String nameAndPathToSave,final String originalFileName) {
        try {
            final RequestBody requestBody = RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize());
            final MediaType mediaType = MediaType.parseMediaType(Files.probeContentType(Paths.get(originalFileName)));
            return uploadFile(nameAndPathToSave, requestBody, mediaType);
        } catch (IOException e) {
            log.error("MultipartFile - s3 업로드 예외", e);
            throw new FileControlException("MultipartFile - s3 업로드 예외", e);
        }
    }

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

    private String uploadFile(final String directoryPath, final RequestBody requestBody, final MediaType mediaType) {
        final String uploadPath = assetRootDirectory + directoryPath;
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uploadPath)
                .contentType(mediaType.toString())
                .build();

        s3Client.putObject(putObjectRequest, requestBody);

        cloudfrontCacheInvalidator.createInvalidation(directoryPath);

        final String uploadedUrl = cloudFrontBaseDomain + directoryPath;
        log.info("File uploaded to S3: {}, published: {}", uploadedUrl, uploadedUrl);
        return uploadedUrl;
    }

    @Override
    public void delete(String accessUrl) throws FileControlException {
        try {
            final String assertActualPath = accessUrl.replaceFirst(cloudFrontBaseDomain, assetRootDirectory);
            final DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(assertActualPath)
                    .build();
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
