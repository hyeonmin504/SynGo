package backend.synGo.util;

import backend.synGo.exception.NotValidException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class LocalDateRangeValidator implements ConstraintValidator<DateTimeRange, LocalDate> {

    private final LocalDate min = LocalDate.of(2025, 5, 28);
    private final LocalDate max = LocalDate.of(9999, 12, 31);

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) return true; // null은 @NotNull로 따로 검증
        boolean pass = !value.isBefore(min) && !value.isAfter(max);
        if (pass)
            return true;
        else
            throw new NotValidException("날자를 선택해주세요");
    }
}
