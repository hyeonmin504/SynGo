package backend.synGo.auth.oauth2;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import backend.synGo.repository.UserOAuthConnectionRepository;
import backend.synGo.repository.UserRepository;
import backend.synGo.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserOAuthConnectionRepository oauthConnectionRepository;
    private final UserRepository userRepository;
    private final ThemeService themeService;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    @Transactional
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
            // 1. OAuth2User에서 임시 CustomUserDetails 추출
            CustomUserDetails tempUserDetails = (CustomUserDetails) authentication.getPrincipal();

            log.info("OAuth2 성공 처리: provider={}, email={}",
                    tempUserDetails.getProvider(), tempUserDetails.getEmail());

            // 2. ✅ User와 UserOAuthConnection을 한 번에 처리
            CustomUserDetails completeUserDetails = processUserAndOAuthConnection(tempUserDetails, authentication);

            // 3. JWT 토큰 생성 (완전한 UserDetails로)
            String accessToken = jwtProvider.createAccessToken(completeUserDetails);
            String refreshToken = jwtProvider.createRefreshToken(completeUserDetails);

            log.info("OAuth2 사용자 처리 완료: userId={}, email={}",
                    completeUserDetails.getUserId(), completeUserDetails.getEmail());

            // 4. 프론트엔드로 토큰과 함께 리다이렉트
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("provider", completeUserDetails.getProvider().name())
                    .build().toUriString();

            log.info("OAuth2 로그인 성공 리다이렉트: {}", targetUrl);
            redirectStrategy.sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생", e);
            handleError(request, response, e);
        }
    }

    /**
     * ✅ UserOAuthConnection 먼저 처리 후 User 생성/업데이트하는 개선된 메서드
     */
    private CustomUserDetails processUserAndOAuthConnection(CustomUserDetails tempUserDetails, Authentication authentication) {

        String email = tempUserDetails.getEmail();
        String name = tempUserDetails.getName();
        String clientIp = tempUserDetails.getLastAccessIp();
        Provider provider = tempUserDetails.getProvider();

        // OAuth 토큰 정보 가져오기
        String refreshToken = getRefreshToken(authentication);
        String accessToken = getAccessToken(authentication);
        String scope = extractScope(authentication);
        LocalDateTime expiresAt = getTokenExpiresAt(authentication);

        log.info("OAuth 토큰 정보: accessToken={}, refreshToken={}, scope={}",
                accessToken != null ? "존재" : "없음",
                refreshToken != null ? "존재" : "없음",
                scope);

        // 1. ✅ UserOAuthConnection 먼저 처리 (생성 또는 업데이트)
        UserOAuthConnection oauthConnection = oauthConnectionRepository
                .findByProvider(provider)
                .map(existingConnection -> {
                    log.info("기존 OAuth 연동 업데이트 시작: connectionId={}", existingConnection.getId());
                    existingConnection.updateOAuthInfo(accessToken, refreshToken, expiresAt, scope, tempUserDetails.getProfileImageUrl());
                    return oauthConnectionRepository.save(existingConnection);
                })
                .orElseGet(() -> {
                    log.info("새 OAuth 연동 생성 시작: provider={}, email={}", provider, email);
                    UserOAuthConnection newConnection = UserOAuthConnection.builder()
                            .provider(provider)
                            .email(email)
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .expiresAt(expiresAt)
                            .scope(scope)
                            .profileImageUrl(tempUserDetails.getProfileImageUrl())
                            .build();
                    return oauthConnectionRepository.save(newConnection);
                });

        // 2. ✅ User 처리 (생성 또는 업데이트) - OAuth 연동 포함
        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    log.info("기존 사용자 발견: {}", email);
                    existingUser.updateOAuth2Info(name, clientIp);
                    // 기존 사용자의 OAuth 연동 업데이트 (필요시)
                    if (existingUser.getUserOAuthConnection() == null ||
                            !existingUser.getUserOAuthConnection().equals(oauthConnection)) {
                        existingUser.setUserOAuthConnection(oauthConnection);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    log.info("새 사용자 생성: {}", email);
                    UserScheduler userScheduler = new UserScheduler(themeService.getTheme(Theme.BLACK));
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .password(null)  // OAuth 사용자는 비밀번호 null
                            .lastAccessIp(clientIp)
                            .scheduler(userScheduler)
                            .userOAuthConnection(oauthConnection)  // ✅ 연관관계 한 번에 설정
                            .build();
                    return userRepository.save(newUser);
                });

        log.info("OAuth2 사용자 및 연동 처리 완료: userId={}, connectionId={}",
                user.getId(), oauthConnection.getId());

        // 3. 완전한 CustomUserDetails 반환 (userId 포함)
        return new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastAccessIp(),
                provider,
                tempUserDetails.getProfileImageUrl()
        );
    }

    /**
     * 에러 처리
     */
    private void handleError(HttpServletRequest request, HttpServletResponse response, Exception e) throws IOException {
        String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", "authentication_processing_error")
                .queryParam("message", e.getMessage())
                .build().toUriString();

        redirectStrategy.sendRedirect(request, response, errorUrl);
    }

    /**
     * RefreshToken 가져오기
     */
    private String getRefreshToken(Authentication authentication) {
        try {
            OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(authentication);

            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                String refreshToken = authorizedClient.getRefreshToken().getTokenValue();
                log.info("RefreshToken 발견: {}...", refreshToken.substring(0, Math.min(10, refreshToken.length())));
                return refreshToken;
            }

            log.warn("RefreshToken을 찾을 수 없습니다. prompt=consent 설정을 확인하세요.");
            return null;

        } catch (Exception e) {
            log.error("RefreshToken 조회 중 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AccessToken 가져오기
     */
    private String getAccessToken(Authentication authentication) {
        try {
            OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(authentication);

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();
                log.info("AccessToken 발견: {}...", accessToken.substring(0, Math.min(10, accessToken.length())));
                return accessToken;
            }

            log.warn("AccessToken을 찾을 수 없습니다.");
            return null;

        } catch (Exception e) {
            log.error("AccessToken 조회 중 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 만료시간 가져오기
     */
    private LocalDateTime getTokenExpiresAt(Authentication authentication) {
        try {
            OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(authentication);

            if (authorizedClient != null && authorizedClient.getAccessToken() != null
                    && authorizedClient.getAccessToken().getExpiresAt() != null) {

                return LocalDateTime.ofInstant(
                        authorizedClient.getAccessToken().getExpiresAt(),
                        java.time.ZoneId.systemDefault()
                );
            }

            // 기본값: 1시간 후
            return LocalDateTime.now().plusHours(1);

        } catch (Exception e) {
            log.error("토큰 만료시간 조회 중 오류: {}", e.getMessage());
            return LocalDateTime.now().plusHours(1);
        }
    }

    /**
     * Scope 추출
     */
    private String extractScope(Authentication authentication) {
        try {
            OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(authentication);

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                return String.join(" ", authorizedClient.getAccessToken().getScopes());
            }

            return "";

        } catch (Exception e) {
            log.error("Scope 추출 중 오류: {}", e.getMessage());
            return "";
        }
    }

    /**
     * OAuth2AuthorizedClient 가져오기 (공통 메서드)
     */
    private OAuth2AuthorizedClient getAuthorizedClient(Authentication authentication) {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken)) {
                log.warn("Authentication이 OAuth2AuthenticationToken이 아닙니다.");
                return null;
            }

            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            if (registrationId == null) {
                log.warn("registrationId를 찾을 수 없습니다.");
                return null;
            }

            // OAuth2AuthorizedClient 조회
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient(registrationId, authentication.getName());

            if (authorizedClient == null) {
                log.warn("OAuth2AuthorizedClient를 찾을 수 없습니다: registrationId={}, principalName={}",
                        registrationId, authentication.getName());
            }

            return authorizedClient;

        } catch (Exception e) {
            log.error("OAuth2AuthorizedClient 조회 중 오류: {}", e.getMessage());
            return null;
        }
    }
}