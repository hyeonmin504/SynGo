package backend.synGo.form.requestForm;

import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.util.DateTimeRange;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SlotForm {

    @Builder.Default
    private String status = "PLAN";
    @DateTimeRange(message = "날짜가 허용된 범위를 벗어났습니다.")
    private LocalDateTime startDate;
    @Nullable
    @DateTimeRange(message = "날짜가 허용된 범위를 벗어났습니다.")
    private LocalDateTime endDate;
    @NotBlank
    private String title;
    @Nullable
    private String content;
    @Nullable
    private String place;
    private SlotImportance importance;
}
