package backend.synGo.webSocket;

import backend.synGo.webSocket.config.RedisPublisher;
import backend.synGo.webSocket.message.GroupSyncMonthMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketTest {

    private WebSocketStompClient stompClient;
    private StompSession session;
    @Autowired
    private RedisPublisher redisPublisher;

    @PostConstruct
    public void testMessage() throws JsonProcessingException {
        GroupSyncMonthMessage test = new GroupSyncMonthMessage(121L,2025,7);
        redisPublisher.publish(new ObjectMapper().writeValueAsString(test));
    }
}
