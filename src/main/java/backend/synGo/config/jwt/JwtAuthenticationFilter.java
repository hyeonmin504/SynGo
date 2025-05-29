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
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredTokenException e){
            request.setAttribute("exception", "TOKEN_EXPIRED"); // 예외 정보를 request에 설정
            throw e;
        }
    }
}