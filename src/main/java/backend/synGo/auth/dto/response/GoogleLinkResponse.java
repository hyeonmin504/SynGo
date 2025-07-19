package backend.synGo.auth.dto.response;

import backend.synGo.domain.user.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "구글 계정 연동 응답")
public class GoogleLinkResponse {

    @Schema(description = "연동 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "구글 계정이 성공적으로 연동되었습니다")
    private String message;

    @Schema(description = "연동된 이메일", example = "user@gmail.com")
    private String linkedEmail;

    @Schema(description = "소셜 제공자", example = "GOOGLE")
    private Provider provider;

    @Schema(description = "프로필 이미지 URL", example = "https://lh3.googleusercontent.com/...")
    private String profileImageUrl;

    @Schema(description = "연동 시간")
    private LocalDateTime linkedAt;

    public static GoogleLinkResponse success(String linkedEmail, String profileImageUrl) {
        return GoogleLinkResponse.builder()
                .success(true)
                .message("구글 계정이 성공적으로 연동되었습니다")
                .linkedEmail(linkedEmail)
                .provider(Provider.GOOGLE)
                .profileImageUrl(profileImageUrl)
                .linkedAt(LocalDateTime.now())
                .build();
    }

    public static GoogleLinkResponse alreadyLinked(String linkedEmail) {
        return GoogleLinkResponse.builder()
                .success(true)
                .message("이미 연동된 구글 계정입니다")
                .linkedEmail(linkedEmail)
                .provider(Provider.GOOGLE)
                .linkedAt(LocalDateTime.now())
                .build();
    }
}