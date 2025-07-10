package backend.synGo.auth.util;

import backend.synGo.form.ResponseForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
                log.info("Authentication failed: {}", authException.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");

                String exception = (String) request.getAttribute("exception");
                String message;

                if ("TOKEN_EXPIRED".equals(exception) || "토큰 만료".equals(exception)) {
                        message = "Access Token이 만료되었습니다.";
                } else {
                        message = "인증이 필요합니다.";
                }

                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now().toString());
                body.put("status", HttpStatus.UNAUTHORIZED.value());
                body.put("error", message);
                body.put("path", request.getRequestURI());
                body.put("method", request.getMethod());

                ResponseForm<Map<String, Object>> responseForm =
                        new ResponseForm<>(HttpStatus.UNAUTHORIZED.value(), body, message);

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper.writeValueAsString(responseForm);

                response.getWriter().write(jsonResponse);
        }
}
