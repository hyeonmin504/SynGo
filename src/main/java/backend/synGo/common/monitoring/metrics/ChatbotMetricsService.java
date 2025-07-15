package backend.synGo.common.monitoring.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatbotMetricsService {

    private final MeterRegistry meterRegistry;

    public ChatbotMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 성공 요청 메트릭 기록
     */
    public void recordSuccessRequest(String apiType, String method, String clientIp, long executionTime) {
        // 1. 요청 성공 카운터
        meterRegistry.counter("chatbot_requests_total",
                        Tags.of("api_type", apiType, "status", "success", "method", method, "client_ip", maskIp(clientIp)))
                .increment();

        // 2. 응답 시간 기록 (일반 API만)
        if (!"stream".equals(apiType)) {
            meterRegistry.timer("chatbot_response_time", Tags.of("api_type", apiType, "method", method))
                    .record(executionTime, TimeUnit.MILLISECONDS);
        }

        // 3. 성능 등급 기록
        String performanceLevel = getPerformanceLevel(executionTime);
        meterRegistry.counter("chatbot_performance_level",
                        Tags.of("api_type", apiType, "level", performanceLevel))
                .increment();

        // 5. 지연 요청 체크
        if (executionTime > 6000) {
            meterRegistry.counter("chatbot_slow_requests_total",
                            Tags.of("api_type", apiType, "threshold", "6000ms"))
                    .increment();
        }

        log.debug("📊 [METRICS] 성공 메트릭 기록 완료 - API: {}, 시간: {}ms, 성능: {}",
                apiType, executionTime, performanceLevel);
    }

    /**
     * 스트리밍 시작 메트릭 기록
     */
    public void recordStreamingStart(String apiType, String method, String clientIp) {
        meterRegistry.counter("chatbot_requests_total",
                        Tags.of("api_type", apiType, "status", "streaming_started", "method", method, "client_ip", maskIp(clientIp)))
                .increment();

        log.debug("📊 [METRICS] 스트리밍 시작 메트릭 기록 - API: {}", apiType);
    }

    /**
     * 에러 메트릭 기록
     */
    public void recordError(String apiType, String method, String clientIp, Exception e, long executionTime) {
        String errorType = e.getClass().getSimpleName();

        // 1. 에러 카운터
        meterRegistry.counter("chatbot_errors_total",
                        Tags.of("api_type", apiType, "error_type", errorType, "method", method, "client_ip", maskIp(clientIp)))
                .increment();

        // 2. 에러 발생 시간 기록
        meterRegistry.timer("chatbot_error_time",
                        Tags.of("api_type", apiType, "error_type", errorType))
                .record(executionTime, TimeUnit.MILLISECONDS);

        log.debug("📊 [METRICS] 에러 메트릭 기록 완료 - API: {}, 에러: {}, 시간: {}ms",
                apiType, errorType, executionTime);
    }

    /**
     * 사용자 정의 메트릭 기록
     */
    public void recordCustomMetric(String metricName, String apiType, String... additionalTags) {
        Tags tagsBuilder = Tags.of("api_type", apiType);

        // 추가 태그가 있다면 추가 (key-value 쌍으로)
        for (int i = 0; i < additionalTags.length - 1; i += 2) {
            if (i + 1 < additionalTags.length) {
                tagsBuilder = tagsBuilder.and(additionalTags[i], additionalTags[i + 1]);
            }
        }

        meterRegistry.counter(metricName, tagsBuilder).increment();

        log.debug("📊 [METRICS] 커스텀 메트릭 기록 - {}: {}", metricName, apiType);
    }

    /**
     * 타이머 샘플 시작
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 타이머 샘플 중지
     */
    public void stopTimer(Timer.Sample sample, String metricName, String apiType, String method) {
        sample.stop(Timer.builder(metricName)
                .tag("api_type", apiType)
                .tag("method", method)
                .register(meterRegistry));
    }

    /**
     * 성능 등급 분류
     */
    private String getPerformanceLevel(long executionTime) {
        if (executionTime < 4000) return "excellent";      // 1초 미만 - 우수
        else if (executionTime < 5000) return "good";      // 1-2초 - 양호
        else if (executionTime < 6000) return "fair";      // 2-4초 - 보통
        else return "poor";                                 // 4초 이상 - 불량
    }

    /**
     * IP 마스킹 (개인정보 보호)
     */
    private String maskIp(String ip) {
        if (ip == null || ip.equals("unknown")) return "unknown";

        if (ip.contains(".")) {
            // IPv4: 192.168.1.100 -> 192.168.*.*
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        } else if (ip.contains(":")) {
            // IPv6: 간단히 앞 두 블록만 유지
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":*:*";
            }
        }

        return "masked";
    }
}