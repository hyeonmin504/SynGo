package backend.synGo.form;

import backend.synGo.domain.slot.SlotImportance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SlotDtoForMonth {
    Long groupId;
    Long slotId;
    String title;
    LocalDateTime startTime;
    SlotImportance importance;
}
