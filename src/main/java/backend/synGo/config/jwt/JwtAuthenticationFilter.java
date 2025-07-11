package backend.synGo.config.jwt;

import backend.synGo.auth.form.TokenType;
import backend.synGo.exception.ExpiredTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final List<String> permitAll;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        PathPatternParser parser = new PathPatternParser();
        String uri = request.getRequestURI();
        log.info("JWT Filter processing: {} (Method: {}, IP: {})",
                uri, request.getMethod(), request.getRemoteAddr());
        //jwt 인증 url 검증
        if (permitAll.stream().anyMatch(pattern ->
                parser.parse(pattern).matches(PathContainer.parsePath(request.getRequestURI())))) {
            log.info("Request URI '{}' is permitted, skipping JWT authentication.", uri);
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = jwtProvider.resolveToken(request);
            if (token != null && jwtProvider.validateToken(token, TokenType.TOKEN)) {
                Authentication auth = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredTokenException ex) {
            // 응답은 여기서 하지 않고 예외 던짐
            // ExceptionTranslationFilter가 처리해서 authenticationEntryPoint 호출
            log.error("Expired token: {}", ex.getMessage());
            throw ex;
        } catch (Exception e) {
            // 다른 예외 처리 (선택 사항)
            log.error("JWT Authentication error: {}", e.getMessage());
            throw e;
        }
    }
}