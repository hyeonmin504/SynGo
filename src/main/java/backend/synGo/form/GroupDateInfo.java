package backend.synGo.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupDateInfo {
    Long groupId;
    @Builder.Default
    List<DateDtoForMonth> monthInfo = new ArrayList<>();
}
