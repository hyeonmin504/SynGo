package backend.synGo.auth.oauth2;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.config.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("OAuth2 인증 성공 처리 시작");

        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. 리다이렉트할 수 없습니다: {}", redirectUri);
            return;
        }

        try {
            // 1. OAuth2User에서 User 정보 추출
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

            // 2. CustomUserDetails 생성 (기존 JWT 시스템과 호환)
            CustomUserDetails userDetails = new CustomUserDetails(
                    oauth2User.getUserId(),
                    oauth2User.getName(),
                    oauth2User.getEmail(),
                    oauth2User.getLastAccessIp(),
                    oauth2User.getProvider(),
                    oauth2User.getProfileImageUrl()
            );

            // 3. 기존 JwtProvider로 JWT 토큰 생성
            String accessToken = jwtProvider.createAccessToken(userDetails);
            String refreshToken = jwtProvider.createRefreshToken(userDetails);

            log.info("OAuth2 사용자 JWT 토큰 생성 완료: {} ({})",
                    oauth2User.getEmail(), oauth2User.getProvider());

            // 4. 프론트엔드로 토큰과 함께 리다이렉트
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", oauth2User.getProvider().name())
                    .build().toUriString();

            log.info("OAuth2 로그인 성공 리다이렉트: {}", targetUrl);
            redirectStrategy.sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);

            // 에러 발생 시 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "authentication_processing_error")
                    .queryParam("message", e.getMessage())
                    .build().toUriString();

            redirectStrategy.sendRedirect(request, response, errorUrl);
        }
    }
}