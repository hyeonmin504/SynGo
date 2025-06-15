package backend.synGo.form;

import backend.synGo.domain.slot.SlotImportance;
import jakarta.annotation.Nullable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Data
public class SlotDtoForDay {
    Long groupId;
    Long slotId;
    String title;
    LocalDateTime startTime;
    LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    SlotImportance importance;
    @Nullable
    Long userGroupId;
    @Nullable
    String editorNickname;
}
