package backend.synGo.common.aop;

import backend.synGo.common.aop.annotation.ChatbotLogging;
import backend.synGo.common.monitoring.metrics.ChatbotMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 챗봇 로깅 AOP - 실제 ChatbotMetricsService와 연동
 */
@Aspect
@Component
@Slf4j
public class ChatbotLoggingAspect {

    private final ChatbotMetricsService metricsService;

    public ChatbotLoggingAspect(ChatbotMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Around("@annotation(chatbotLogging)")
    public Object logChatbotExecution(ProceedingJoinPoint joinPoint, ChatbotLogging chatbotLogging) throws Throwable {

        String apiType = chatbotLogging.apiType();

        // 스트리밍과 일반 모드 구분 처리
        if ("stream".equals(apiType)) {
            return handleStreamingMode(joinPoint, chatbotLogging);
        } else {
            return handleNormalMode(joinPoint, chatbotLogging);
        }
    }

    /**
     * 일반 모드 처리 - 전체 라이프사이클 추적
     */
    private Object handleNormalMode(ProceedingJoinPoint joinPoint, ChatbotLogging chatbotLogging) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String apiType = chatbotLogging.apiType();
        String methodName = joinPoint.getSignature().getName();
        String clientIp = getClientIp();

        // 요청 로깅
        if (chatbotLogging.logRequest()) {
            log.info("🤖 [REQ-{}] 챗봇 요청 시작 - API: {}, Method: {}, IP: {}",
                    requestId, apiType, methodName, clientIp);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 실제 메서드 실행
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // 응답 로깅
            log.info("🤖 [RES-{}] 챗봇 응답 완료 ({}ms) - API: {}",
                    requestId, executionTime, apiType);

            // 📊 메트릭 기록 (새로운 인터페이스 사용)
            metricsService.recordSuccessRequest(apiType, methodName, clientIp, executionTime);

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [ERR-{}] 챗봇 에러 ({}ms) - API: {}, 에러: {}",
                    requestId, executionTime, apiType, e.getMessage());

            // 📊 에러 메트릭 기록
            metricsService.recordError(apiType, methodName, clientIp, e, executionTime);

            throw e;
        }
    }

    /**
     * 스트리밍 모드 처리 - 시작만 추적
     */
    private Object handleStreamingMode(ProceedingJoinPoint joinPoint, ChatbotLogging chatbotLogging) throws Throwable {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String apiType = chatbotLogging.apiType();
        String methodName = joinPoint.getSignature().getName();
        String clientIp = getClientIp();

        // 요청 로깅
        if (chatbotLogging.logRequest()) {
            log.info("🌊 [REQ-{}] 스트리밍 요청 시작 - API: {}, Method: {}, IP: {}",
                    requestId, apiType, methodName, clientIp);
        }

        try {
            // SSE 연결 생성
            Object result = joinPoint.proceed();

            // 스트리밍 연결 성공 로깅
            log.info("🌊 [START-{}] 스트리밍 연결 성공 - 클라이언트로 데이터 전송 시작",
                    requestId);

            // 📊 스트리밍 메트릭 기록
            metricsService.recordStreamingStart(apiType, methodName, clientIp);

            return result;

        } catch (Exception e) {
            log.error("❌ [ERR-{}] 스트리밍 연결 실패 - API: {}, 에러: {}",
                    requestId, apiType, e.getMessage());

            // 📊 스트리밍 에러 메트릭 기록 (실행시간 0으로)
            metricsService.recordError(apiType, methodName, clientIp, e, 0L);

            throw e;
        }
    }

    /**
     * 클라이언트 IP 조회
     */
    private String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}