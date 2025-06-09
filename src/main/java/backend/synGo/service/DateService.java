package backend.synGo.service;

import backend.synGo.config.scheduler.SchedulerProvider;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static backend.synGo.controller.date.DateSearchController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateService {

    private final SchedulerProvider schedulerProvider;
    private final UserGroupRepository userGroupRepository;
    private final DateRepository dateRepository;

    @Transactional(readOnly = true)
    public GetGroupDateInfo getDatesForMonthInGroup(Long groupId, int year, int month, Long requesterUserId) {
        if(!userGroupRepository.existsByGroupIdAndUserId(groupId,requesterUserId)) {
            throw new AccessDeniedException("그룹원 외 접근 불가");
        }
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //다음 달을 조회 한 경우
        boolean isNextMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue()+1;
        //Redis 캐시 조회
        Optional<GetGroupDateInfo> cachedSchedule = schedulerProvider.getSchedule(groupId, year, month);
        //캐시 존재 && 이번 달 혹은 다음 달 인 경우
        if ((isCurrentMonth || isNextMonth) && cachedSchedule.isPresent()) {
            log.info("캐시 조회중");
            return cachedSchedule.get();
        }
        //DB 조회
        List<Date> dateByMonth = findDateByMonth(year, month, groupId);
        //dto 작성
        List<DateInfo> dateInfo = dateByMonth.stream()
                .map(DateService::getDateInfoToDto)
                .collect(Collectors.toList());

        GetGroupDateInfo getGroupDateInfo = new GetGroupDateInfo(groupId, dateInfo);
        if (isCurrentMonth || isNextMonth) {
            schedulerProvider.saveGroupScheduler(groupId, getGroupDateInfo, year, month);
            log.info("데이터 캐싱");
            return getGroupDateInfo;
        }
        return getGroupDateInfo;
    }

    @Transactional(readOnly = true)
    private List<Date> findDateByMonth(int year, int month, Long groupId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        // 예시: 이 범위 내의 데이터를 조회
        return dateRepository.findScheduleDateWithSlotsByGroupAndDateRange(groupId, startDate, endDate);
    }

    /**
     * user이 생성한 date의 SlotCount, summary를 업데이트 todo: 그룹 슬롯과 유저 슬롯을 통합해서 summary를 결정하기
     * @param date
     * @param userSlot
     * @return
     */
    @Deprecated // TODO: 6/5/25 user,group 두개 슬롯 포함한 업데이트로 변경
    public void updateDateInfo(Date date, UserSlot userSlot) {
        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
        if (date.getSlotCount() != 0){
            List<UserSlot> allSlot = date.getUserSlot();
            allSlot.add(userSlot);
            //중요도로 내림차순 정렬한다
            allSlot.sort(Comparator.comparingInt((UserSlot s) -> s.getImportance().getPriority()).reversed());

            //최신화 문제를 예방하기 위해 안전하게 접근
            String summary = allSlot.stream()
                    .limit(2)
                    .map(UserSlot::getTitle)
                    .collect(Collectors.joining(","));

            date.addSlotCountAndSummary(summary);
        }
        else
            date.addSlotCountAndSummary(userSlot.getTitle());
    }

    /**
     * user이 생성한 date의 SlotCount, summary를 업데이트
     * @param date
     * @param groupSlot
     * @return
     */
    @Deprecated // TODO: 6/5/25  이건 date 조회 시 설정 하도록 위치 변경 예정
    public void updateGroupDateInfo(Date date, GroupSlot groupSlot) {
        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
        if (date.getSlotCount() != 0){
            log.info("date.getSlotCount()={}",date.getSlotCount());
            List<GroupSlot> allSlot = date.getGroupSlot();
            allSlot.add(groupSlot);
            //중요도로 내림차순 정렬한다
            allSlot.sort(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed());

            //최신화 문제를 예방하기 위해 안전하게 접근
            String summary = allSlot.stream()
                    .limit(2)
                    .map(GroupSlot::getTitle)
                    .collect(Collectors.joining(","));

            date.addSlotCountAndSummary(summary);
        }
        else
            date.addSlotCountAndSummary(groupSlot.getTitle());
    }

    private static DateInfo getDateInfoToDto(Date date) {
        return DateInfo.builder()
                .dateId(date.getId())
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(
                        date.getGroupSlot().stream()
                                .map(DateService::getSlotInfoToDto)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static SlotInfo getSlotInfoToDto(GroupSlot groupSlot) {
        return SlotInfo.builder()
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }
}
