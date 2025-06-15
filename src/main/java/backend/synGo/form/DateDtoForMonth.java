package backend.synGo.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class DateDtoForMonth {
    int slotCount;
    LocalDate today;
    @Builder.Default
    List<SlotDtoForMonth> slotInfo = new ArrayList<>();
}
