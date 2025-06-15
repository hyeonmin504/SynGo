package backend.synGo.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DateDtoForDay {
    int slotCount;
    LocalDate today;
    @Builder.Default
    List<SlotDtoForDay> slotInfo = new ArrayList<>();
}