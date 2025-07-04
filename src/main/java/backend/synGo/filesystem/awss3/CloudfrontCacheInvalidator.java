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

    private static final String COMPLETED = "Completed";
    private static final String CREATE_INVALIDATION_RESULT_LOG_FORMAT = "CloudFront create invalidation - id : {} , status: {}";

    private final CloudFrontClient cloudFrontClient;

    @Value("${aws.cloud-front.asset-distribution-id}")
    private String distributionId;

    public void createInvalidation(final String... paths) {
        final InvalidationBatch invalidationBatch = createInvalidationBatch(paths);
        final CreateInvalidationRequest request = createInvalidationRequest(invalidationBatch);
        final CreateInvalidationResponse response = cloudFrontClient.createInvalidation(request);
    }

    private CreateInvalidationRequest createInvalidationRequest(final InvalidationBatch invalidationBatch) {
        return CreateInvalidationRequest.builder()
                .distributionId(distributionId)
                .invalidationBatch(invalidationBatch)
                .build();
    }

    private InvalidationBatch createInvalidationBatch(final String[] paths) {
        final List<String> items = List.of(paths);
        return InvalidationBatch.builder()
                .paths(
                        Paths.builder()
                                .items(items)
                                .quantity(items.size())
                                .build()
                )
                .callerReference(UUID.randomUUID().toString())
                .build();
    }

    private void logResult(final CreateInvalidationResponse response) {
        final Invalidation invalidation = response.invalidation();
        final String status = invalidation.status();
        if (COMPLETED.equals(status)) {
            log.info(CREATE_INVALIDATION_RESULT_LOG_FORMAT, invalidation.id(), status);
            return ;
        }
        log.error(CREATE_INVALIDATION_RESULT_LOG_FORMAT, invalidation.id(), status);
    }
}
