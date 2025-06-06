package backend.synGo;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class TestScenario {
    public String leaderToken;
    public String memberToken;
    public Long groupId;
    public Long slotId;
    public Long memberUserGroupId;
}