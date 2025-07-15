package backend.synGo.common.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 챗봇 API 로깅을 위한 어노테이션
 * 기본 버전 - 최소한의 기능만 포함
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatbotLogging {

    /**
     * API 타입 구분 (예: "normal", "stream")
     */
    String apiType() default "normal";

    /**
     * 요청 로깅 여부
     */
    boolean logRequest() default true;

    /**
     * 응답 로깅 여부
     */
    boolean logResponse() default true;
}