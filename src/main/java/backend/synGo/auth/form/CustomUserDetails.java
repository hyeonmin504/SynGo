package backend.synGo.auth.form;

import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static backend.synGo.domain.user.Provider.LOCAL;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, AuthenticatedUser {

    private final Long userId;
    private final String name;
    private final String email;
    private final String lastAccessIp;
    private final Provider provider;
    private final String profileImageUrl;

    // 기존 생성자 유지 (하위 호환성)
    public CustomUserDetails(Long userId, String name, String lastAccessIp) {
        this.userId = userId;
        this.name = name;
        this.email = null;
        this.lastAccessIp = lastAccessIp;
        this.provider = LOCAL;
        this.profileImageUrl = null;
    }

    // OAuth2를 위한 새 생성자
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.lastAccessIp = user.getLastAccessIp();
        this.provider = user.getProvider();
        this.profileImageUrl = user.getProfileImageUrl();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_BASIC");
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return getName();
    }

    public String getUserLastAccessIp() {
        return getLastAccessIp();
    }

    // AuthenticatedUser 인터페이스 구현
    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getLastAccessIp() {
        return lastAccessIp;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}