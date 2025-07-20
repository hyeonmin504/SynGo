// SocialInfoResponse.java
package backend.synGo.auth.dto.response;

import backend.synGo.domain.user.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "연동된 소셜 정보 응답")
public class SocialInfoResponse {

    @Schema(description = "연동 여부", example = "true")
    private boolean isLinked;

    @Schema(description = "소셜 제공자", example = "GOOGLE")
    private Provider provider;

    @Schema(description = "연동된 이메일", example = "user@gmail.com")
    private String email;

    @Schema(description = "프로필 이미지 URL", example = "https://lh3.googleusercontent.com/...")
    private String profileImageUrl;

    public static SocialInfoResponse notLinked() {
        return SocialInfoResponse.builder()
                .isLinked(false)
                .build();
    }

    public static SocialInfoResponse linked(Provider provider, String email,
                                            String profileImageUrl) {
        return SocialInfoResponse.builder()
                .isLinked(true)
                .provider(provider)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}