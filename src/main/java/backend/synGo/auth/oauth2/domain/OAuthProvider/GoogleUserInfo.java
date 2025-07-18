package backend.synGo.auth.oauth2.domain.OAuthProvider;

import lombok.Data;

// Google 사용자 정보
@Data
public class GoogleUserInfo implements ProviderUserInfo {
    private String id;
    private String email;
    private String name;
    private String picture; // 프로필 이미지
    private String locale;
    private boolean verified_email;



    @Override
    public String getProfileImageUrl() {
        return picture;
    }

    @Override
    public String getNickname() {
        return name;
    }

    @Override
    public String getGender() {
        return null;
    }

    @Override
    public String getAgeRange() {
        return null;
    }

    @Override
    public String getBirthday() {
        return null;
    }

    @Override
    public String getMobile() {
        return null;
    }
}
