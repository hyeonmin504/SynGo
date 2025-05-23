package backend.synGo.config.jwt;

import backend.synGo.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final List<String> permitUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        //요청 헤더에서 토큰 가져오기
        String token = resolveToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            try {
                Authentication authentication = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                //로그 확인 용
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                log.info("userDetails.getUsername={}",userDetails.getUsername());
            } catch (SecurityException e) {
                // 인증 실패 시 로그만 찍고, 인증은 안 한 상태로 넘어감
                log.warn("Authentication failed={}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7).trim(); // 토큰의 "Bearer " 제거 후 반환
        }
        return null;
    }


}

