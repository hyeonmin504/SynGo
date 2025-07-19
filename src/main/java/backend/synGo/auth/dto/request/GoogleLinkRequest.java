package backend.synGo.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "구글 계정 연동 요청")
public class GoogleLinkRequest {

    @NotBlank(message = "인증 코드는 필수입니다")
    @Schema(description = "구글 OAuth 인증 코드", example = "4/0AdQt8qh...")
    private String code;

    @Schema(description = "state 파라미터 (선택사항)", example = "random_state_string")
    private String state;
}