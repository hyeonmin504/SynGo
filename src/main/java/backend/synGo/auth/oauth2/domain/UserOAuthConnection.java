package backend.synGo.auth.oauth2.domain;

import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_oauth_connections")
public class UserOAuthConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_oauth_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @OneToMany(mappedBy = "userOAuthConnection")
    @Builder.Default
    private List<User> user = new ArrayList<>();
    @Email
    private String email;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String scope;
    private String profileImageUrl; // 프로필 이미지 URL

    // OAuth 정보 업데이트 (RefreshToken 있을 때만 업데이트)
    public void updateOAuthInfo(String accessToken, String refreshToken,
                                LocalDateTime expiresAt, String scope, String profileImageUrl) {
        this.accessToken = accessToken;

        // RefreshToken이 있을 때만 업데이트 (없으면 기존 것 유지)
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            this.refreshToken = refreshToken;
        }

        this.expiresAt = expiresAt;
        this.scope = scope;
        this.profileImageUrl = profileImageUrl;
    }

    // 토큰 만료 체크
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public void setUser(User user) {
        this.user.add(user);
        user.setUserOAuthConnection(this);
    }
}