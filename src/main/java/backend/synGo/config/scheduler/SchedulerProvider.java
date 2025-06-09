package backend.synGo.config.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static backend.synGo.controller.date.DateSearchController.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerProvider {

    @Qualifier("groupScheduleRedisTemplate")
    private final RedisTemplate<String, GetGroupDateInfo> redisTemplate;

    private static final String Key = "GROUP:";

    @Value("${security.scheduler.group.expiration}")
    private long saveGroupDataMinutes;

    @Value("${security.scheduler.user.expiration}")
    private long saveUserDataMinutes;

    private String getRedisKey(Long groupId, int year, int month) {
        return "GROUP:" + groupId + ":" + year + ":" + month;
    }

    public void saveGroupScheduler(Long groupId, GetGroupDateInfo getGroupDateInfo, int year, int month) {
        String key = getRedisKey(groupId, year, month);
        Duration duration = Duration.ofMinutes(saveGroupDataMinutes);
        redisTemplate.opsForValue().set(key, getGroupDateInfo, duration);
        log.info("Saved scheduler group={} to Redis.", groupId);
    }

    public Optional<GetGroupDateInfo> getSchedule(Long groupId, int year, int month) {
        String key = getRedisKey(groupId, year, month);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void evictSchedule(Long groupId, int year, int month) {
        String key = getRedisKey(groupId, year, month);
        redisTemplate.delete(key);
        log.debug("Deleted schedule group={} from Redis.", groupId);
    }
}