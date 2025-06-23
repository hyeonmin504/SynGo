package backend.synGo.webSocket.config;

import backend.synGo.webSocket.handler.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker   // 메세지 브로커를 통해 웹소켓 메시지 처리를 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // STOMP 프로토콜을 사용하여 웹소켓 메시지를 처리하기 위한 설정 클래스입니다.
    private final StompHandler stompHandler;

    /**
     * 메세지 브로커를 설정합니다.
     * @param config
     */
    @Override
     public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub"); // 클라이언트가 구독할 수 있는 주제(prefix)
        config.setApplicationDestinationPrefixes("/pub"); // @MessageMapping의 접두사
     }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp") // 클라이언트가 연결할 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 설정, 모든 출처 허용
                .withSockJS(); // SockJS를 사용하여 폴백 옵션 제공
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler); // STOMP 메시지 전송 전에 StompHandler를 사용하여 처리
    }
}
