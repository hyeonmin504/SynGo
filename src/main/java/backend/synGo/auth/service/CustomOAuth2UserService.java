package backend.synGo.auth.service;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import backend.synGo.repository.UserRepository;
import backend.synGo.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final ThemeService themeService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 로그인 프로세스 시작");

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // ✅ 전체 응답 디버깅
        log.info("OAuth2 전체 응답: {}", oauth2User.getAttributes());

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = getProviderByRegistrationId(registrationId);

        // ✅ 각 필드별 디버깅
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String profileImageUrl = oauth2User.getAttribute("picture");

        log.info("=== OAuth2 필드별 디버깅 ===");
        log.info("  - email: {}", email);
        log.info("  - name: {}", name);
        log.info("  - picture: {}", profileImageUrl);
        log.info("  - 전체 키 목록: {}", oauth2User.getAttributes().keySet());

        // ✅ nameAttributeKey 확인
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        log.info("  - nameAttributeKey: {}", nameAttributeKey);

        String clientIp = getClientIp();

        // ✅ User만 처리
        User user = processUser(email, name, clientIp);

        log.info("OAuth2 사용자 처리 완료: {} ({})", user.getEmail(), provider);

        // ✅ CustomUserDetails 바로 반환
        return new CustomUserDetails(user, provider, profileImageUrl, oauth2User.getAttributes());
    }

    /**
     * User만 생성/조회
     */
    private User processUser(String email, String name, String clientIp) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    log.info("기존 사용자 발견: {}", email);
                    existingUser.updateOAuth2Info(name, clientIp);
                    return existingUser;
                })
                .orElseGet(() -> {
                    log.info("새 사용자 생성: {}", email);
                    UserScheduler userScheduler = new UserScheduler(themeService.getTheme(Theme.BLACK));
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .password(null)
                            .lastAccessIp(clientIp)
                            .scheduler(userScheduler)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private Provider getProviderByRegistrationId(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return Provider.GOOGLE;
            default:
                throw new IllegalArgumentException("지원되지 않는 OAuth 제공자: " + registrationId);
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
            return "unknown";
        } catch (Exception e) {
            log.warn("클라이언트 IP 주소를 가져올 수 없습니다: {}", e.getMessage());
            return "unknown";
        }
    }
}