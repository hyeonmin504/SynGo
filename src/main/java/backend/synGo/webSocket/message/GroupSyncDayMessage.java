package backend.synGo.webSocket.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSyncDayMessage {
    private Long groupId;
    private int year;
    private int month;
    private int day;
}
