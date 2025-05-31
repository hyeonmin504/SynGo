package backend.synGo.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Data
public class GroupsPagingForm {
    private Long groupId;
    private Long groupUserId;
    private LocalDateTime createDate;
    private String name;
    private String information;
    private String nickname;
}
