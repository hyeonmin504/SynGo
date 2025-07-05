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
            filterChain.doFilter(request, response);
            return;
        }
        try {
            //인증 url 토큰 검사 시작
            String token = jwtProvider.resolveToken(request);
            if (token != null && jwtProvider.validateToken(token, TokenType.TOKEN)) {
                Authentication authentication = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info(">> Authentication success. User: {}", authentication.getName());
            } else {
                log.warn(">> JWT 인증 실패 또는 토큰 없음.");
            }
            filterChain.doFilter(request, response);
        } catch (Exception e){
            log.info("Token expired: {}", e.getMessage());
            request.setAttribute("exception", "TOKEN_EXPIRED"); // 예외 정보를 request에 설정
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Authentication failed\"}");
            }
            throw e;
        }
    }
}