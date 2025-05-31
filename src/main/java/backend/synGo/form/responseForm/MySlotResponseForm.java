package backend.synGo.form.responseForm;

import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.util.DateTimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "개인 슬롯 응답 DTO")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MySlotResponseForm {

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
    private String status;
}
