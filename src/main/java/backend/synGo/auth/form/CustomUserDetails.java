package backend.synGo.auth.form;

import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Long userId;
    private final String name;
    private final String email;
    private final String lastAccessIp;
    private final Provider provider;
    private final String profileImageUrl;

    // ✅ 일반 로그인용 생성자 (기존)
    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.lastAccessIp = user.getLastAccessIp();
        if(user.getUserOAuthConnection() != null){
            log.info("provider={}",user.getUserOAuthConnection().getProvider());
            this.provider = user.getUserOAuthConnection().getProvider();
        }
        else this.provider = Provider.LOCAL; // 로컬 로그인
        this.profileImageUrl = null;
    }

    // ✅ JWT 인증용 생성자 (기존)
    public CustomUserDetails(Long userId, String name, String lastAccessIp, String email, String profileImageUrl,String provider) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.lastAccessIp = lastAccessIp;
        this.profileImageUrl = profileImageUrl;
        this.provider = Provider.valueOf(provider);
    }

    @Override
    public <A> A getAttribute(String name) {
        return OAuth2User.super.getAttribute(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
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