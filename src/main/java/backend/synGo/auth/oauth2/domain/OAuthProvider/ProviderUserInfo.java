package backend.synGo.auth.oauth2.domain.OAuthProvider;

import lombok.Data;

// 공통 인터페이스
public interface ProviderUserInfo {
    String getEmail();
    String getName();
    String getProfileImageUrl();
    String getNickname();
    String getGender();
    String getAgeRange();
    String getBirthday();
    String getMobile();
}

