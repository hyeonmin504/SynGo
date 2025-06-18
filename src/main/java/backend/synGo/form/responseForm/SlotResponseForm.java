package backend.synGo.form.responseForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "그룹 슬롯 응답 DTO")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotResponseForm {

    private Long slotId;
    @Schema(description = "제목", example = "스터디 일정")
    private String title;

    @Schema(description = "내용", example = "자바 스터디 모임")
    private String content;

    @Schema(description = "시작 시간", example = "2025-06-01T10:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료 시간", example = "2025-06-01T12:00:00")
    private LocalDateTime endDate;

    @Schema(description = "생성 시간", example = "2025-05-31T09:00:00")
    private LocalDateTime createDate;

    @Schema(description = "장소", example = "카페 드롭탑")
    private String place;

    @Schema(description = "중요도", example = "HIGH")
    private String importance;

    @Schema(description = "상태", example = "CONFIRMED")
    @Pattern(regexp = "COMPLETE|DELAY|DOING|PLAN|CANCEL|HOLD", message = "올바른 상태 값을 입력하세요.")
    @NotBlank
    private String status;

    private String updater;

    @Builder.Default
    private List<JoinMemberForm> member = new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Data
    public static class JoinMemberForm {
        Long joinMemberId;
        String nickname;
        String permission;
    }
}
