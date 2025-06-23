package backend.synGo.webSocket.config;

import backend.synGo.webSocket.message.GroupSyncDayMessage;
import backend.synGo.webSocket.message.GroupSyncDetailMessage;
import backend.synGo.webSocket.message.GroupSyncMonthMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    public void updatedData(String publishMessage) {
        try {
            JsonNode root = objectMapper.readTree(publishMessage);

            // 메시지 종류 판별
            if (root.has("slotId")) {
                // 상세 슬롯
                log.info("Received a slot view message: {}", root);
                GroupSyncDetailMessage message = objectMapper.treeToValue(root, GroupSyncDetailMessage.class);
                messagingTemplate.convertAndSend(
                        "/sub/groups/" + message.getGroupId() + "/slots/" + message.getSlotId(),
                        message
                );
            } else if (root.has("day")) {
                // 하루 뷰
                log.info("Received a day view message: {}", root);
                GroupSyncDayMessage message = objectMapper.treeToValue(root, GroupSyncDayMessage.class);
                messagingTemplate.convertAndSend(
                        "/sub/groups/" + message.getGroupId() + "/date/day?year=" + message.getYear() + "&month=" + message.getMonth() + "&day=" + message.getDay(),
                        message
                );
            } else {
                // 한 달 뷰
                log.info("Received a month view message: {}", root);
                GroupSyncMonthMessage message = objectMapper.treeToValue(root, GroupSyncMonthMessage.class);
                messagingTemplate.convertAndSend(
                        "/sub/groups/" + message.getGroupId() + "/date/month?year=" + message.getYear() + "&month=" + message.getMonth(),
                        message
                );
            }

        } catch (Exception e) {
            log.error("RedisSubscriber - 메시지 처리 중 예외 발생", e);
        } finally {
            // ✅ 로그 추가
            log.info("[SUBSCRIBE] Received message: {}", publishMessage);
        }
    }
}
