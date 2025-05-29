package backend.synGo.util;

import backend.synGo.exception.NotValidException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 커스텀 LocalDateTime 검증 에노테이션 Validation Min Max 검증
 * @DateRange(min = "2024-01-01", max = "2025-12-31")
 * private LocalDate date;
 */
public class LocalDateTimeRangeValidator implements ConstraintValidator<DateTimeRange, LocalDateTime> {

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(2025, 5, 28, 0, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !value.isBefore(MIN_DATE) && !value.isAfter(MAX_DATE);
    }
}
