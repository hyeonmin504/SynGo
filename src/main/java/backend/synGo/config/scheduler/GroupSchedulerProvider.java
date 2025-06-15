package backend.synGo.config.scheduler;

import backend.synGo.form.DateDtoForMonth;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupSchedulerProvider {

    @Qualifier("groupScheduleRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${security.scheduler.group.expiration}")
    private long saveGroupDataMinutes;

    @Value("${security.scheduler.user.expiration}")
    private long saveUserDataMinutes;

    private String getGroupRedisKey(Long groupId, int year, int month) {
        return "GROUP:" + groupId + ":" + year + ":" + month;
    }
    private String getMyGroupRedisKey(Long userId, int year, int month) {
        return "MY_GROUP:" + userId + ":" + year + ":" + month;
    }
    private String getMyRedisKey(Long userId, int year, int month) {
        return "MY:" + userId + ":" + year + ":" + month;
    }

    public void saveGroupScheduler(Long groupId, List<DateDtoForMonth> dateDtoForMonths, int year, int month) {
        String key = getGroupRedisKey(groupId, year, month);
        Duration duration = Duration.ofMinutes(saveGroupDataMinutes);
        redisTemplate.opsForValue().set(key, dateDtoForMonths, duration);
        log.info("그룹 데이터 캐싱");
    }
    public void saveMyGroupScheduler(Long userId, List<DateDtoForMonth> dateDtoForMonths, int year, int month) {
        String key = getMyGroupRedisKey(userId, year, month);
        Duration duration = Duration.ofMinutes(saveUserDataMinutes);
        redisTemplate.opsForValue().set(key, dateDtoForMonths, duration);
        log.info("유저의 그룹 데이터 캐싱");
    }
    public void saveMyScheduler(Long userId, List<DateDtoForMonth> dateDtoForMonths, int year, int month) {
        String key = getMyRedisKey(userId, year, month);
        Duration duration = Duration.ofMinutes(saveUserDataMinutes);
        redisTemplate.opsForValue().set(key, dateDtoForMonths, duration);
        log.info("유저 데이터 캐싱");
    }

    public List<DateDtoForMonth> getGroupSchedule(Long groupId, int year, int month) {
        Object value = redisTemplate.opsForValue().get(getGroupRedisKey(groupId, year, month));
        if (value == null) return Collections.emptyList();
        log.info("그룹 데이터 조회");
        return objectMapper.convertValue(value, new TypeReference<>(){});
    }
    public List<DateDtoForMonth> getMyGroupSchedule(Long userId, int year, int month) {
        Object value = redisTemplate.opsForValue().get(getMyGroupRedisKey(userId, year, month));
        if (value == null) return Collections.emptyList();
        log.info("유저의 그룹 데이터 캐싱");
        return objectMapper.convertValue(value, new TypeReference<>(){});
    }
    public List<DateDtoForMonth> getMySchedule(Long userId, int year, int month) {
        Object value = redisTemplate.opsForValue().get(getMyRedisKey(userId, year, month));
        if (value == null) return Collections.emptyList();
        log.info("유저 데이터 캐싱");
        return objectMapper.convertValue(value, new TypeReference<>(){});
    }

    public void evictGroupSchedule(Long groupId, int year, int month) {
        String key = getGroupRedisKey(groupId, year, month);
        redisTemplate.delete(key);
        log.info("그룹 캐시 삭제 groupId={}", groupId);
    }
    public void evictMyGroupSchedule(Long userId, int year, int month) {
        String key = getMyGroupRedisKey(userId, year, month);
        redisTemplate.delete(key);
        log.info("유저 그룹 캐시 삭제 userId={}", userId);
    }
    public void evictMySchedule(Long userId, int year, int month) {
        String key = getMyRedisKey(userId, year, month);
        redisTemplate.delete(key);
        log.info("유저 캐시 삭제 userId={}", userId);
    }

    public boolean isSameYearAndMonth(LocalDate requestDay) {
        log.info("이번 달 입니다");
        return requestDay.getYear() == LocalDate.now().getYear() &&
                requestDay.getMonth() == LocalDate.now().getMonth();
    }

    public boolean isSameYearAndMonthPlusOne(LocalDate requestDay) {
        log.info("다음 달 입니다");
        return requestDay.getYear() == LocalDate.now().plusMonths(1).getYear() &&
                requestDay.getMonth() == LocalDate.now().plusMonths(1).getMonth();
    }
}