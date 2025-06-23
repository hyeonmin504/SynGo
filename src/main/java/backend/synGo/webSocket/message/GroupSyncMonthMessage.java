package backend.synGo.webSocket.message;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupSyncMonthMessage {
    private Long groupId;
    private int year;
    private int month;
}
