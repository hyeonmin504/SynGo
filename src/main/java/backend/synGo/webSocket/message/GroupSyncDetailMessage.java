package backend.synGo.webSocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSyncDetailMessage {
    private Long groupId;
    private Long slotId;
}
