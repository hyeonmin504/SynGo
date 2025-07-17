package backend.synGo.auth.service;

import backend.synGo.auth.oauth2.CustomOAuth2User;
import backend.synGo.auth.oauth2.OAuthAttributes;
import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import backend.synGo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 로그인 프로세스 시작");

        // 1. 기본 OAuth2UserService로 OAuth2User 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        // 2. OAuth2 제공자 정보 추출
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google", "naver" 등
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName(); // OAuth2 로그인 시 키가 되는 필드 (구글: "sub")

        log.info("OAuth2 제공자: {}, userNameAttribute: {}", registrationId, userNameAttributeName);

        // 3. OAuth2UserService를 통해 가져온 정보를 우리 서비스에 맞는 dto로 변환
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId,
                userNameAttributeName,
                oauth2User.getAttributes()
        );

        // 4. 현재 요청의 IP 주소 가져오기
        String clientIp = getClientIp();

        // 5. 사용자 저장/업데이트
        User user = saveOrUpdate(attributes, clientIp, getProviderByRegistrationId(registrationId));

        log.info("OAuth2 사용자 처리 완료: {} ({})", user.getEmail(), user.getProvider());

        // 6. CustomOAuth2User 반환
        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_BASIC")),
                attributes.getAttributes(),
                attributes.getNameAttributeKey(),
                user
        );
    }

    /**
     * 사용자 저장 또는 업데이트
     */
    private User saveOrUpdate(OAuthAttributes attributes, String clientIp, Provider provider) {
        // 1. 먼저 OAuth 제공자 + 제공자 ID로 찾기 (가장 정확한 방법)
        return userRepository.findByProviderAndProviderId(provider, attributes.getProviderId())
                .map(existingUser -> {
                    // 기존 사용자 더티체킹으로 업데이트
                    log.info("OAuth 제공자 ID로 기존 사용자 찾음: {}", existingUser.getEmail());
                    existingUser.updateOAuth2Info(attributes.getName(), attributes.getPicture());
                    existingUser.updateLastAccessIp(clientIp);
                    // 트랜잭션 종료 시 더티체킹으로 자동 업데이트
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 2. OAuth 제공자 ID로 못 찾으면, 이메일 + 제공자로 찾기
                    return userRepository.findByEmailAndProvider(attributes.getEmail(), provider)
                            .map(existingUser -> {
                                // 기존 사용자 더티체킹으로 업데이트
                                log.info("이메일로 기존 OAuth 사용자 찾음: {}", existingUser.getEmail());
                                existingUser.updateOAuth2Info(attributes.getName(), attributes.getPicture());
                                existingUser.updateLastAccessIp(clientIp);
                                // providerId가 없었다면 설정 (User 엔티티에 setter 메소드 필요)
                                return existingUser;
                            })
                            .orElseGet(() -> {
                                // 3. 완전히 새로운 사용자 생성 후 저장
                                log.info("새 OAuth2 사용자 생성: {}", attributes.getEmail());
                                User newUser = attributes.toEntity(clientIp, provider);
                                return userRepository.save(newUser); // 새 엔티티는 명시적 save 필요
                            });
                });
    }
    /**
     * registrationId를 Provider enum으로 변환
     */
    private Provider getProviderByRegistrationId(String registrationId) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return Provider.GOOGLE;
            case "naver":
                return Provider.NAVER;
            case "facebook":
                return Provider.FACEBOOK;
            default:
                throw new IllegalArgumentException("지원되지 않는 OAuth 제공자: " + registrationId);
        }
    }

    /**
     * 클라이언트 IP 주소 가져오기
     */
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