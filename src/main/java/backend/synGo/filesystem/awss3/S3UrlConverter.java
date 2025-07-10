package backend.synGo.filesystem.awss3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
@Slf4j
public class S3UrlConverter {
    @Value("${aws.cloud-front.domain}")
    private String cloudFrontBaseDomain;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * CloudFront URL 또는 S3 URL을 S3 key로 변환
     */
    public String convertUrlToS3Key(String url) {
        if (url.contains("cloudfront.net")) {
            return convertCloudFrontUrlToS3Key(url);
        } else if (url.contains(".s3.") && url.contains(".amazonaws.com")) {
            return convertS3UrlToS3Key(url);
        } else {
            throw new IllegalArgumentException("지원하지 않는 URL 형식: " + url);
        }
    }

    /**
     * CloudFront URL을 S3 key로 변환
     */
    private String convertCloudFrontUrlToS3Key(String cloudFrontUrl) {
        String cleanCloudFrontDomain = cloudFrontBaseDomain
                .replace("https://", "")
                .replace("http://", "");

        // 정규식으로 도메인 제거 (// 처리 포함)
        String pattern = "https?://" + Pattern.quote(cleanCloudFrontDomain) + "/+";
        String s3Key = cloudFrontUrl.replaceFirst(pattern, "");

        return validateAndReturnS3Key(s3Key);
    }

    /**
     * S3 URL을 S3 key로 변환
     */
    private String convertS3UrlToS3Key(String s3Url) {
        // S3 URL 패턴: https://bucket.s3.region.amazonaws.com//path/to/file
        String pattern = "https?://" + Pattern.quote(bucketName) + "\\.s3\\.[^/]+\\.amazonaws\\.com/+";
        String s3Key = s3Url.replaceFirst(pattern, "");

        return validateAndReturnS3Key(s3Key);
    }

    /**
     * S3 key 유효성 검증 및 정리
     */
    public String validateAndReturnS3Key(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            throw new IllegalArgumentException("S3 key가 비어있습니다.");
        }

        log.debug("변환 전 S3 key: '{}'", s3Key);

        // 1. 연속된 슬래시를 하나로 정리
        s3Key = s3Key.replaceAll("/+", "/");

        // 2. 맨 끝의 슬래시만 제거 (앞의 슬래시는 유지)
        s3Key = s3Key.replaceAll("/+$", "");

        log.debug("정리 후 S3 key: '{}'", s3Key);

        // 3. 빈 문자열 재검증
        if (s3Key.isEmpty()) {
            throw new IllegalArgumentException("정리된 S3 key가 비어있습니다.");
        }

        // 4. 위험한 패턴 체크
        if (s3Key.contains("..")) {
            throw new IllegalArgumentException("S3 key에 '..'이 포함될 수 없습니다: " + s3Key);
        }

        // 5. 슬래시로 시작하는지 재확인 로직 제거 (슬래시로 시작해야 함)

        // 6. 버킷명이 포함되어 있는지 확인
        if (bucketName != null && s3Key.contains(bucketName)) {
            throw new IllegalArgumentException("S3 key에 버킷명이 포함되어서는 안됩니다: " + s3Key);
        }

        log.debug("S3 key 검증 통과: '{}'", s3Key);
        return s3Key;
    }
}