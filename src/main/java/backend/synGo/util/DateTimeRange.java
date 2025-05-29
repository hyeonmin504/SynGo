package backend.synGo.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 커스텀 LocalDateTime 에노테이션
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {
        LocalDateRangeValidator.class,
        LocalDateTimeRangeValidator.class
})
public @interface DateTimeRange {
    String message() default "날짜가 허용된 범위를 벗어났습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}