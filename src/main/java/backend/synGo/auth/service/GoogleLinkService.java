package backend.synGo.auth.service;

import backend.synGo.auth.dto.request.GoogleLinkRequest;
import backend.synGo.auth.dto.response.GoogleLinkResponse;
import backend.synGo.auth.dto.response.SocialInfoResponse;
import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import backend.synGo.exception.AccountTokenException;
import backend.synGo.exception.ExpiredTokenException;
import backend.synGo.repository.UserOAuthConnectionRepository;
import backend.synGo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleLinkService {

    private final UserRepository userRepository;
    private final UserOAuthConnectionRepository userOAuthConnectionRepository;
    private final UserOAuthConnectionRepository oauthConnectionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String linkRedirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    @Transactional
    public GoogleLinkResponse linkGoogleAccount(CustomUserDetails userDetails, GoogleLinkRequest request, String returnIrl) {
        try {
            log.info("구글 계정 연동 시작: userId={}, returnUrl={}", userDetails.getUserId(), returnIrl);

            // 1. Authorization Code로 Access Token 획득
            GoogleTokenResponse tokenResponse = getGoogleAccessToken(request.getCode(), returnIrl);

            // 2. Access Token으로 사용자 정보 조회
            GoogleUserInfo googleUserInfo = getGoogleUserInfo(tokenResponse.getAccessToken());

            // 3. 기존 연동 확인 및 처리
            return processGoogleLink(userDetails, tokenResponse, googleUserInfo);

        } catch (Exception e) {
            log.error("구글 계정 연동 중 오류 발생: userId={}, error={}",
                    userDetails.getUserId(), e.getMessage(), e);
            throw new RuntimeException("구글 계정 연동에 실패했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SocialInfoResponse getSocialInfo(CustomUserDetails userDetails) {
        if(userDetails.getProvider() == Provider.LOCAL) {
            return SocialInfoResponse.builder()
                    .isLinked(false)
                    .provider(Provider.LOCAL)
                    .email(userDetails.getEmail())
                    .profileImageUrl(userDetails.getProfileImageUrl())
                    .build();
        }
        log.info("소셜 정보 조회: userId={}", userDetails.getUserId());
        Optional<UserOAuthConnection> connection = userOAuthConnectionRepository.findByConnectionUserId(userDetails.getUserId());
        if (connection.isPresent()){
            return SocialInfoResponse.linked(
                    connection.get().getProvider(),
                    connection.get().getEmail(),
                    connection.get().getProfileImageUrl()
            );
        }
        return SocialInfoResponse.builder()
                .isLinked(false)
                .provider(Provider.LOCAL)
                .email(userDetails.getEmail())
                .profileImageUrl(userDetails.getProfileImageUrl())
                .build();

    }

    /**
     *
     * @param userId
     * @param returnUrl
     * @return
     */
    public String generateGoogleOAuthUrl(Long userId, String returnUrl) {
        log.info("구글 OAuth URL 생성: userId={}, returnUrl={}", userId, returnUrl);

        // returnUrl이 있으면 그것을 사용하고, 없으면 기본값 사용
        String redirectUri = (returnUrl != null && !returnUrl.isEmpty()) ?
                returnUrl : linkRedirectUri;

        log.info("최종 redirect_uri: {}", redirectUri);

        String url = UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", "link_" + userId)
                .build()
                .toUriString();

        log.info("생성된 OAuth URL: {}", url);
        return url;
    }

    /**
     * 처음 토큰 발급 시
     * @param authorizationCode
     * @param returnUrl
     * @return
     */
    private GoogleTokenResponse getGoogleAccessToken(String authorizationCode, String returnUrl) {
        log.info("구글 Access Token 요청 시작");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 2. redirectUri 변수 선언 추가
        String redirectUri = (returnUrl != null && !returnUrl.isEmpty()) ?
                returnUrl : linkRedirectUri;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri); // 현재 페이지 기준

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                return GoogleTokenResponse.builder()
                        .accessToken(jsonNode.get("access_token").asText())
                        .refreshToken(jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null)
                        .expiresIn(jsonNode.get("expires_in").asLong())
                        .scope(jsonNode.has("scope") ? jsonNode.get("scope").asText() : "")
                        .build();
            } else {
                throw new RuntimeException("구글 토큰 요청 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("구글 Access Token 요청 실패", e);
            throw new RuntimeException("구글 토큰 요청에 실패했습니다", e);
        }
    }

    /**
     * refresh -> 토큰 재발급
     * 이미 라이브러리 내에 재발급 기능이 구현되어 있음
     */
//    @Transactional
//    public void refreshAccessTokenIfNeeded(UserOAuthConnection oAuthConnection) {
//        if (oAuthConnection.isExpired()) {
//            log.info("토큰이 만료되어 새로 발급받습니다. User: {}", oAuthConnection.getEmail());
//
//            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//            params.add("client_id", googleClientId);
//            params.add("client_secret", googleClientSecret);
//            params.add("refresh_token", oAuthConnection.getRefreshToken());
//            params.add("grant_type", "refresh_token");
//
//            try {
//                String response = webClient.post()
//                        .uri("https://oauth2.googleapis.com/token")
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .bodyValue(params)
//                        .retrieve()
//                        .bodyToMono(String.class)
//                        .block();
//
//                JsonNode jsonNode = objectMapper.readTree(response);
//
//                String newAccessToken = jsonNode.get("access_token").asText();
//                long expiresIn = jsonNode.get("expires_in").asLong();
//                LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(expiresIn);
//
//                // DB 업데이트
//                oAuthConnection.updateOAuthInfo(
//                        newAccessToken,
//                        null, // refresh token은 그대로 유지
//                        newExpiresAt,
//                        oAuthConnection.getScope(),
//                        oAuthConnection.getProfileImageUrl()
//                );
//
//                log.info("토큰 갱신 완료");
//            } catch (Exception e) {
//                log.error("토큰 갱신 실패", e);
//                throw new AccountTokenException("토큰 갱신에 실패했습니다", e);
//            }
//        }
//    }

    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        log.info("구글 사용자 정보 요청 시작");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                return GoogleUserInfo.builder()
                        .email(jsonNode.get("email").asText())
                        .name(jsonNode.get("name").asText())
                        .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                        .build();
            } else {
                throw new RuntimeException("구글 사용자 정보 요청 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("구글 사용자 정보 요청 실패", e);
            throw new RuntimeException("구글 사용자 정보 요청에 실패했습니다", e);
        }
    }

    private GoogleLinkResponse processGoogleLink(CustomUserDetails userDetails,
                                                 GoogleTokenResponse tokenResponse,
                                                 GoogleUserInfo googleUserInfo) {

        log.info("구글 연동 처리: userId={}, googleEmail={}",
                userDetails.getUserId(), googleUserInfo.getEmail());

        // 1. 현재 사용자 조회
        User currentUser = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 2. 해당 구글 계정으로 기존 UserOAuthConnection 조회
        Optional<UserOAuthConnection> existingConnection = oauthConnectionRepository
                .findByProviderAndEmail(Provider.GOOGLE, googleUserInfo.getEmail());

        if (existingConnection.isPresent()) {
            // 3-1. 기존 연동이 있는 경우 - 현재 사용자를 추가
            UserOAuthConnection connection = existingConnection.get();

            // 이미 현재 사용자가 연동되어 있는지 확인
            boolean alreadyLinked = connection.getUser().stream()
                    .anyMatch(user -> user.getId().equals(currentUser.getId()));

            if (alreadyLinked) {
                log.info("이미 연동된 구글 계정: userId={}, googleEmail={}",
                        userDetails.getUserId(), googleUserInfo.getEmail());
                return GoogleLinkResponse.alreadyLinked(googleUserInfo.getEmail());
            }

            // 토큰 정보 업데이트 및 사용자 추가
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
            connection.updateOAuthInfo(
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    expiresAt,
                    tokenResponse.getScope(),
                    googleUserInfo.getPicture()
            );
            currentUser.setUserOAuthConnection(connection);

            oauthConnectionRepository.save(connection);

        } else {
            // 3-2. 새로운 연동 생성
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());

            UserOAuthConnection newConnection = UserOAuthConnection.builder()
                    .provider(Provider.GOOGLE)
                    .email(googleUserInfo.getEmail())
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .expiresAt(expiresAt)
                    .scope(tokenResponse.getScope())
                    .profileImageUrl(googleUserInfo.getPicture())
                    .build();

            currentUser.setUserOAuthConnection(newConnection);
            oauthConnectionRepository.save(newConnection);
        }

        log.info("구글 계정 연동 완료: userId={}, googleEmail={}",
                userDetails.getUserId(), googleUserInfo.getEmail());

        return GoogleLinkResponse.success(googleUserInfo.getEmail(), googleUserInfo.getPicture());
    }

    // 내부 클래스들
    @lombok.Builder
    @lombok.Getter
    private static class GoogleTokenResponse {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
        private String scope;
    }

    @lombok.Builder
    @lombok.Getter
    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
    }
}