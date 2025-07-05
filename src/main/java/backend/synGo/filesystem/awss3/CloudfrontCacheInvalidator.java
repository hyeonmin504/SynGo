package backend.synGo.filesystem.awss3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CloudfrontCacheInvalidator {

    // CloudFront 무효화 요청 완료 상태
    private static final String COMPLETED = "Completed";

    // 로그 출력 포맷 (invalidation ID와 상태)
    private static final String CREATE_INVALIDATION_RESULT_LOG_FORMAT =
            "CloudFront create invalidation - id={} , status={}";

    // AWS CloudFront SDK 클라이언트 (v2)
    private final CloudFrontClient cloudFrontClient;

    // CloudFront 배포 ID (환경에서 주입받음)
    @Value("${aws.cloud-front.asset-distribution-id}")
    private String distributionId;

    /**
     * CloudFront에 캐시 무효화 요청을 전송하는 메서드
     * @param paths 무효화 대상 파일 경로 ("/path/to/file.jpg" 같은 경로)
     */
    public void createInvalidation(final String... paths) {
        // 1. InvalidationBatch 생성
        final InvalidationBatch invalidationBatch = createInvalidationBatch(paths);

        // 2. CloudFront 무효화 요청 생성
        final CreateInvalidationRequest request = createInvalidationRequest(invalidationBatch);

        // 3. AWS SDK 호출 → 실제 무효화 요청 수행
        final CreateInvalidationResponse response = cloudFrontClient.createInvalidation(request);

        // 4. 결과 로그 출력 (성공/실패 여부 포함)
        logResult(response);
    }

    /**
     * CloudFront 무효화 요청 객체 생성
     */
    private CreateInvalidationRequest createInvalidationRequest(final InvalidationBatch invalidationBatch) {
        return CreateInvalidationRequest.builder()
                .distributionId(distributionId)
                .invalidationBatch(invalidationBatch)
                .build();
    }

    /**
     * CloudFront InvalidationBatch 생성
     * @param paths 캐시 무효화 대상 파일 경로 배열
     * @return InvalidationBatch 객체
     */
    private InvalidationBatch createInvalidationBatch(final String[] paths) {
        final List<String> items = List.of(paths); // 가변 인자를 리스트로 변환
        return InvalidationBatch.builder()
                .paths(
                        Paths.builder()
                                .items(items)               // 무효화할 경로들
                                .quantity(items.size())     // 경로 개수
                                .build()
                )
                .callerReference(UUID.randomUUID().toString()) // 요청 유니크 ID
                .build();
    }

    /**
     * 무효화 결과 로그 출력
     * @param response AWS 응답 객체
     */
    private void logResult(final CreateInvalidationResponse response) {
        final Invalidation invalidation = response.invalidation();
        final String status = invalidation.status();
        // 상태가 Completed인 경우 → info 로그
        if (COMPLETED.equals(status)) {
            log.info(CREATE_INVALIDATION_RESULT_LOG_FORMAT, invalidation.id(), status);
            return;
        }
        // 그 외의 경우 (InProgress 등) → error 로그
        log.error(CREATE_INVALIDATION_RESULT_LOG_FORMAT, invalidation.id(), status);
    }
}