package backend.synGo.form.requestForm;

import backend.synGo.domain.group.GroupType;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupRequestForm {

    @NotBlank
    private String groupName;
    @NotBlank
    private String nickname;
    @NotBlank
    private String info;
    @Nullable
    @Size(min = 4, max = 16)
    private String password;
    @Nullable
    private String checkPassword;
    private GroupType groupType;
}
