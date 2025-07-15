package backend.synGo.chatBot.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DateTimeTools {

    @Bean
    @Description("상대적 날짜와 시간 표현을 실제 날짜시간으로 변환합니다. 예: '내일 오후2시', '3일 후 아침 9시', '모레 저녁 7시'")
    public Function<DateTimeRequest, DateTimeResponse> calculateDateTime() {
        log.info("DateTimeTool -날자 변환 사용중");
        return request -> {
            LocalDate today = LocalDate.now();
            LocalDate targetDate = parseRelativeDate(today, request.expression());
            LocalTime targetTime = parseTime(request.expression());

            LocalDateTime result = LocalDateTime.of(targetDate, targetTime);
            return new DateTimeResponse(
                    result.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
        };
    }

    @Bean
    @Description("현재 날짜와 시간을 YYYY-MM-DD HH:mm 형식으로 반환합니다")
    public Supplier<DateTimeResponse> getCurrentDateTime() {
        log.info("DateTimeTools -날자 형식 변환 사용중");
        return () -> new DateTimeResponse(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * 상대적 날짜 표현을 현재 날짜를 기준으로 계산합니다.
     * 예: "오늘", "내일", "모레", "3일 후", "2주 후" 등
     */
    private LocalDate parseRelativeDate(LocalDate baseDate, String expression) {
        String expr = expression.toLowerCase().trim();

        if (expr.contains("오늘")) return baseDate;
        if (expr.contains("내일")) return baseDate.plusDays(1);
        if (expr.contains("모레")) return baseDate.plusDays(2);
        if (expr.contains("일주일 후")) return baseDate.plusWeeks(1);

        // "3일 후" 패턴
        Pattern dayPattern = Pattern.compile("(\\d+)일\\s*후");
        Matcher dayMatcher = dayPattern.matcher(expr);
        if (dayMatcher.find()) {
            int days = Integer.parseInt(dayMatcher.group(1));
            return baseDate.plusDays(days);
        }

        // "2주 후" 패턴
        Pattern weekPattern = Pattern.compile("(\\d+)주\\s*후");
        Matcher weekMatcher = weekPattern.matcher(expr);
        if (weekMatcher.find()) {
            int weeks = Integer.parseInt(weekMatcher.group(1));
            return baseDate.plusWeeks(weeks);
        }

        return baseDate; // 기본값은 오늘
    }

    /**
     * 시간 표현을 파싱하여 LocalTime 객체로 변환합니다.
     * 예: "오후2시", "오전9시", "저녁7시", "밤11시", "새벽6시", "14시30분" 등
     */
    private LocalTime parseTime(String expression) {
        String expr = expression.toLowerCase().trim();

        // 오후/오전 패턴
        Matcher pmMatcher = Pattern.compile("오후\\s*(\\d{1,2})시").matcher(expr);
        if (pmMatcher.find()) {
            int hour = Integer.parseInt(pmMatcher.group(1));
            if (hour == 12) return LocalTime.of(12, 0);
            return LocalTime.of(hour + 12, 0);
        }

        Matcher amMatcher = Pattern.compile("오전\\s*(\\d{1,2})시").matcher(expr);
        if (amMatcher.find()) {
            int hour = Integer.parseInt(amMatcher.group(1));
            if (hour == 12) return LocalTime.of(0, 0);
            return LocalTime.of(hour, 0);
        }

        // 저녁 (18-22시로 추정)
        Matcher eveningMatcher = Pattern.compile("저녁\\s*(\\d{1,2})시").matcher(expr);
        if (eveningMatcher.find()) {
            int hour = Integer.parseInt(eveningMatcher.group(1));
            if (hour <= 10) hour += 12; // 저녁 7시 = 19시
            return LocalTime.of(hour, 0);
        }

        // 아침 (6-11시로 추정)
        if (expr.contains("아침")) {
            Matcher morningMatcher = Pattern.compile("아침\\s*(\\d{1,2})시").matcher(expr);
            if (morningMatcher.find()) {
                int hour = Integer.parseInt(morningMatcher.group(1));
                return LocalTime.of(hour, 0);
            }
            return LocalTime.of(9, 0); // 기본 아침 시간
        }

        // 점심 (12-14시로 추정)
        if (expr.contains("점심")) {
            return LocalTime.of(12, 0);
        }

        // 숫자만 있는 경우 (14시, 15시30분 등)
        Matcher timeMatcher = Pattern.compile("(\\d{1,2})시\\s*(\\d{1,2})분").matcher(expr);
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = Integer.parseInt(timeMatcher.group(2));
            return LocalTime.of(hour, minute);
        }

        Matcher hourMatcher = Pattern.compile("(\\d{1,2})시").matcher(expr);
        if (hourMatcher.find()) {
            int hour = Integer.parseInt(hourMatcher.group(1));
            return LocalTime.of(hour, 0);
        }

        // 기본값: 현재 시간 + 1시간 (적당한 미래 시간)
        return LocalTime.now().plusHours(1);
    }

    public record DateTimeRequest(String expression) {}
    public record DateTimeResponse(String datetime) {}
}