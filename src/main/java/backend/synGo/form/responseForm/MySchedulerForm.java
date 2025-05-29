package backend.synGo.form.responseForm;

import backend.synGo.domain.schedule.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MySchedulerForm {
    public Theme theme;
}
