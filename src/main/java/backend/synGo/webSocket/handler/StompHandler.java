package backend.synGo.webSocket.handler;

import backend.synGo.config.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class StompHandler implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null) {
                token = token.trim();  // 공백 제거
                // Bearer 접두어 제거 (필요시)
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }

            log.info("token={}", token);

            if (token == null || !jwtProvider.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or missing JWT token");
            }
        }
        return message;
    }
}
