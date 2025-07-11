package backend.synGo.config.sse;

import backend.synGo.auth.form.TokenType;
import backend.synGo.config.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@AllArgsConstructor
public class SseAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 🎯 SSE API에만 동작하도록 필터링
        if (!uri.equals("/api/my/chatbot/stream")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔐 Authorization 헤더나 access_token 쿼리에서 토큰 추출
        String token = jwtProvider.resolveToken(request);
        if (token == null) {
            token = request.getParameter("access_token");
        }

        if (token != null && jwtProvider.validateToken(token, TokenType.TOKEN)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 비동기 인증 유지
            request.setAttribute("SPRING_SECURITY_CONTEXT", context);
        }

        filterChain.doFilter(request, response);
    }
}
