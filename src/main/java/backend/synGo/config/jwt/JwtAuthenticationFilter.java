package backend.synGo.config.jwt;

import backend.synGo.auth.controller.form.ResponseForm;
import backend.synGo.auth.form.TokenType;
import backend.synGo.exception.AuthenticationFailedException;
import backend.synGo.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final List permitAll;

    private final GlobalExceptionHandler exceptionHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //요청 헤더에서 토큰 가져오기
        String token = jwtProvider.resolveToken(request);

        if (token != null) {
            try {
                if (jwtProvider.validateToken(token, TokenType.TOKEN)) {
                    Authentication authentication = jwtProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response); // 인증 성공 시 다음 필터로 이동
                    return;
                } else {
                    // 토큰 유효하지 않음
                    throw new AuthenticationFailedException("토큰이 유효하지 않습니다");
                }
            } catch (RuntimeException e) {
                // 예외를 직접 처리 (response에 JSON 반환)
                sendErrorResponse(response, e, request);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, Exception ex, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("method", request.getMethod());

        ResponseForm<Map<String, Object>> responseForm =
                new ResponseForm<>(HttpStatus.UNAUTHORIZED.value(), body, "인증에 실패했습니다");

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(responseForm);

        response.getWriter().write(jsonResponse);
    }
}