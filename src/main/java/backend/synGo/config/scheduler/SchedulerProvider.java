package backend.synGo.config.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static backend.synGo.controller.date.GroupDateSearchController.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerProvider {

    @Qualifier("groupScheduleRedisTemplate")
    private final RedisTemplate<String, GroupDateInfo> redisTemplate;

    private static final String Key = "GROUP:";

    @Value("${security.scheduler.group.expiration}")
    private long saveGroupDataMinutes;

    @Value("${security.scheduler.user.expiration}")
    private long saveUserDataMinutes;

    private String getGroupRedisKey(Long groupId, int year, int month) {
        return "GROUP:" + groupId + ":" + year + ":" + month;
    }

    public void saveGroupScheduler(Long groupId, GroupDateInfo groupDateInfo, int year, int month) {
        String key = getGroupRedisKey(groupId, year, month);
        Duration duration = Duration.ofMinutes(saveGroupDataMinutes);
        redisTemplate.opsForValue().set(key, groupDateInfo, duration);
        log.info("Saved scheduler group={} to Redis.", groupId);
    }

    public Optional<GroupDateInfo> getGroupSchedule(Long groupId, int year, int month) {
        String key = getGroupRedisKey(groupId, year, month);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void evictGroupSchedule(Long groupId, int year, int month) {
        String key = getGroupRedisKey(groupId, year, month);
        redisTemplate.delete(key);
        log.debug("Deleted schedule group={} from Redis.", groupId);
    }


}