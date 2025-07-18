package backend.synGo.auth.oauth2;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
public class OAuthAttributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final String picture;
    private final String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
                           String name, String email, String picture, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName,
                                     Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> ofGoogle(userNameAttributeName, attributes);
            case "naver" -> ofNaver(userNameAttributeName, attributes);
            case "facebook" -> ofFacebook(userNameAttributeName, attributes);
            default -> throw new IllegalArgumentException("Unsupported registrationId: " + registrationId);
        };
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name(getStringAttribute(attributes, "name"))
                .email(getStringAttribute(attributes, "email"))
                .picture(getStringAttribute(attributes, "picture"))
                .providerId(getStringAttribute(attributes, "sub"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // ✅ 안전한 타입 캐스팅
        Map<String, Object> response = getMapAttribute(attributes, "response");

        if (response == null) {
            log.warn("Naver response is null");
            throw new IllegalArgumentException("Invalid Naver OAuth response");
        }

        return OAuthAttributes.builder()
                .name(getStringAttribute(response, "name"))
                .email(getStringAttribute(response, "email"))
                .picture(getStringAttribute(response, "profile_image"))
                .providerId(getStringAttribute(response, "id"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofFacebook(String userNameAttributeName, Map<String, Object> attributes) {
        // ✅ 안전한 타입 캐스팅 - Facebook 프로필 이미지 URL 추출
        String pictureUrl = extractFacebookPictureUrl(attributes);

        return OAuthAttributes.builder()
                .name(getStringAttribute(attributes, "name"))
                .email(getStringAttribute(attributes, "email"))
                .picture(pictureUrl)
                .providerId(getStringAttribute(attributes, "id"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    /**
     * 안전한 String 속성 추출
     */
    private static String getStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * 안전한 Map 속성 추출
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMapAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Map) {
            try {
                return (Map<String, Object>) value;
            } catch (ClassCastException e) {
                log.warn("Failed to cast {} to Map<String, Object>: {}", key, e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Facebook 프로필 이미지 URL 안전하게 추출
     */
    private static String extractFacebookPictureUrl(Map<String, Object> attributes) {
        try {
            // attributes.get("picture") -> Map
            Map<String, Object> picture = getMapAttribute(attributes, "picture");
            if (picture == null) {
                return null;
            }

            // picture.get("data") -> Map
            Map<String, Object> data = getMapAttribute(picture, "data");
            if (data == null) {
                return null;
            }

            // data.get("url") -> String
            return getStringAttribute(data, "url");

        } catch (Exception e) {
            log.warn("Failed to extract Facebook picture URL: {}", e.getMessage());
            return null;
        }
    }
}