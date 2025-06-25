package backend.synGo.webSocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {
    private final ChannelTopic channelTopic;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 이벤트 발생 시 Redis에 메시지를 생성해서 모든 서버에 브로드 캐스팅을 한다.
     * @param message
     */
    public void publish(Object message) {
        // Redis에 메시지를 발행 -> RedisConfig의 RedisMessageListenerContainer가 이 메시지를 수신
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
        // ✅ 로그 추가
        log.info("[PUBLISH] topic: {}, message: {}", channelTopic.getTopic(), message);
    }
}
