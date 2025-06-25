package backend.synGo.webSocket.service;

import backend.synGo.webSocket.config.RedisPublisher;
import backend.synGo.webSocket.message.GroupSyncDayMessage;
import backend.synGo.webSocket.message.GroupSyncDetailMessage;
import backend.synGo.webSocket.message.GroupSyncMonthMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@RequiredArgsConstructor
@Service
public class GroupSyncService {

    private final RedisPublisher redisPublisher;

    /**
     * 슬롯 status 수정 시, 일반 맴버 등록 시 디테일만 동기화
     * @param groupId
     * @param slotId
     */
    public void groupSlotSyncGoPub(Long groupId, Long slotId) {
        log.info("슬롯 status 수정 시, 일반 맴버 등록 시 디테일만 동기화");
        // 예: 상세 슬롯 변경
        GroupSyncDetailMessage detailMessage = new GroupSyncDetailMessage(groupId, slotId);
        redisPublisher.publish(detailMessage);
    }

    /**
     * 슬롯 생성 시 달, 하루 동기화
     * @param groupId
     * @param startDate
     */
    public void groupCreateSyncGoPub(Long groupId, LocalDate startDate) {
        log.info("슬롯 생성 시 달, 하루 동기화");
        // 예: 그룹 슬롯 생성 후
        GroupSyncMonthMessage monthMessage = new GroupSyncMonthMessage(groupId, startDate.getYear(), startDate.getMonthValue());
        redisPublisher.publish(monthMessage);
        // 예: 특정 날자의 변경
        GroupSyncDayMessage dayMessage = new GroupSyncDayMessage(groupId, startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth());
        redisPublisher.publish(dayMessage);
    }

    /**
     * 에디터 등록 시 하루, 디테일 동기화
     * @param groupId
     * @param startDate
     * @param slotId
     */
    public void groupMemberSyncGoPub(Long groupId,LocalDate startDate, Long slotId) {
        log.info("에디터 등록 시 하루, 디테일 동기화");
        // 예: 특정 날자의 변경
        GroupSyncDayMessage dayMessage = new GroupSyncDayMessage(groupId, startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth());
        redisPublisher.publish(dayMessage);
        // 예: 상세 슬롯 변경
        GroupSyncDetailMessage detailMessage = new GroupSyncDetailMessage(groupId, slotId);
        redisPublisher.publish(detailMessage);
    }

    /**
     * 슬롯 업데이트 시 전체 동기화
     * @param groupId
     * @param startDate
     * @param slotId
     */
    public void groupUpdateSlotSyncGoPub(Long groupId, LocalDate startDate, Long slotId) {
        log.info("슬롯 업데이트 시 전체 동기화");
        // 예: 그룹 슬롯 생성 후
        GroupSyncMonthMessage monthMessage = new GroupSyncMonthMessage(groupId, startDate.getYear(), startDate.getMonthValue());
        redisPublisher.publish(monthMessage);
        // 예: 특정 날자의 변경
        GroupSyncDayMessage dayMessage = new GroupSyncDayMessage(groupId, startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth());
        redisPublisher.publish(dayMessage);
        // 예: 상세 슬롯 변경
        GroupSyncDetailMessage detailMessage = new GroupSyncDetailMessage(groupId, slotId);
        redisPublisher.publish(detailMessage);
    }
}
