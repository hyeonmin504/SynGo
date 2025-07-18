package backend.synGo.auth.form;

import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Long userId;
    private final String name;
    private final String email;
    private final String lastAccessIp;
    private final Provider provider;
    private final String profileImageUrl;
    private final Map<String, Object> attributes;

    // ✅ OAuth2 로그인용 생성자 (UserOAuthConnection 없이)
    public CustomUserDetails(User user, Provider provider,
                             String profileImageUrl, Map<String, Object> attributes) {
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.lastAccessIp = user.getLastAccessIp();
        this.provider = provider;
        this.profileImageUrl = profileImageUrl;
        this.attributes = attributes;
    }

    // ✅ 일반 로그인용 생성자 (기존)
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.lastAccessIp = user.getLastAccessIp();
        this.provider = Provider.LOCAL; // 로컬 로그인
        this.profileImageUrl = null;
        this.attributes = Map.of();
    }

    // ✅ JWT 인증용 생성자 (기존)
    public CustomUserDetails(Long userId, String name, String lastAccessIp, String email, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.lastAccessIp = lastAccessIp;
        this.provider = Provider.LOCAL;
        this.profileImageUrl = profileImageUrl;
        this.attributes = Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_BASIC");
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getName() { return name; }
}