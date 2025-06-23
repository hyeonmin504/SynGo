package backend.synGo.webSocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {
    private final ChannelTopic channelTopic;
    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Object message) {
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
        // ✅ 로그 추가
        log.info("[PUBLISH] topic: {}, message: {}", channelTopic.getTopic(), message);
    }
}
