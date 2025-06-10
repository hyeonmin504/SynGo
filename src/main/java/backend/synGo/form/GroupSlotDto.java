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
public class GroupSlotDto {
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

    public GroupSlotDto updateDate(GroupSlotDto dto) {
        this.userGroupId = dto.getUserGroupId();
        this.editorNickname = dto.getEditorNickname();
        return this;
    }

    public GroupSlotDto(Long slotId, @Nullable Long userGroupId, @Nullable String editorNickname) {
        this.slotId = slotId;
        this.userGroupId = userGroupId;
        this.editorNickname = editorNickname;
    }

    public GroupSlotDto(Long slotId, String title, LocalDateTime startTime, LocalDateTime endTime, SlotImportance importance) {
        this.slotId = slotId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.importance = importance;
    }
}
