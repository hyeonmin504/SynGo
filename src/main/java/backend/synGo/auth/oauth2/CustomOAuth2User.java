package backend.synGo.auth.oauth2;

import backend.synGo.auth.form.AuthenticatedUser;
import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User, AuthenticatedUser {
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private User user;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes, String nameAttributeKey, User user) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.user = user;
    }

    @Override
    public String getName() {
        return (String) attributes.get(nameAttributeKey);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // AuthenticatedUser 인터페이스 구현
    @Override
    public Long getUserId() {
        return user.getId();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getLastAccessIp() {
        return user.getLastAccessIp();
    }

    @Override
    public Provider getProvider() {
        return user.getProvider();
    }

    @Override
    public String getProfileImageUrl() {
        return user.getProfileImageUrl();
    }
}