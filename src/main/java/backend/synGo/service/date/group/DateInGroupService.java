package backend.synGo.service.date.group;

import backend.synGo.config.scheduler.GroupSchedulerProvider;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.form.*;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.GroupSlotRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateInGroupService {

    private final GroupSchedulerProvider groupSchedulerProvider;
    private final UserGroupRepository userGroupRepository;
    private final DateRepository dateRepository;
    private final GroupSlotRepository groupSlotRepository;


    @Transactional(readOnly = true)
    public List<DateDtoForMonth> getDatesForMonthInGroup(Long groupId, int year, int month, Long requesterUserId) {
        //인증 조회
        if(!userGroupRepository.existsByGroupIdAndUserId(groupId,requesterUserId)) {
            throw new AccessDeniedException("그룹원 외 접근 불가");
        }
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //다음 달을 조회 한 경우
        boolean isNextMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue()+1;
        //Redis 캐시 조회
        List<DateDtoForMonth> cachedSchedule = groupSchedulerProvider.getGroupSchedule(groupId, year, month);
        //캐시 존재 && 이번 달 혹은 다음 달 인 경우
        if ((isCurrentMonth || isNextMonth) && !cachedSchedule.isEmpty()) {
            log.info("캐시 조회중");
            return cachedSchedule;
        }
        //DB 조회
        List<Date> dateByMonth = findGroupDataByMonth(year, month, groupId);
        //dto 작성
        List<DateDtoForMonth> monthDateDto = dateByMonth.stream()
                .map(DateInGroupService::getMonthDataToDtoLimit2)
                .toList();
        if ((isCurrentMonth || isNextMonth) && !monthDateDto.isEmpty() ) {
            groupSchedulerProvider.saveGroupScheduler(groupId, monthDateDto, year, month);
            log.info("데이터 캐싱");
        }
        return monthDateDto;
    }

    @Transactional(readOnly = true)
    private List<Date> findGroupDataByMonth(int year, int month, Long groupId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        // startDate ~ endDate 전날 까지 Date 모두 조회
        return dateRepository.findScheduleDateWithSlotsByGroupAndMonthRange(groupId, startDate, endDate);
    }

    /**
     * 그룹 슬롯 하루 데이터 조회
     * @param groupId
     * @param year
     * @param month
     * @param day
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public DateDtoForDay findGroupDataByDayInGroup(Long groupId, int year, int month, int day, Long requestUserId) {
        if(userGroupRepository.existsByGroupIdAndUserId(groupId,requestUserId)) {
            //요청한 날자
            LocalDate localDate = LocalDate.of(year,month,day);
            //선택한 date 조회
            Optional<Date> optionalDate = dateRepository.findByGroupIdAndDay(groupId, localDate);

            if (optionalDate.isPresent()) {
                log.info("optionalDate.isPresent()");
                return findAllUserDate(optionalDate.get());
            }
            return new DateDtoForDay();
        }
        throw new AccessDeniedException("그룹원 외 접근 불가");
    }

    @Transactional(readOnly = true)
    private DateDtoForDay findAllUserDate(Date date) {
        // 해당 date의 slotmember, usergroup 조회
        List<SlotDtoForDay> slotDtoForDayList = groupSlotRepository.findByGroupIdAndDay(date.getId());

        return DateDtoForDay.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(slotDtoForDayList)
                .build();
    }

    /**
     * date에 있는 slot을 2개만 가져오는 로직
     * @param date
     * @return
     */
    public static DateDtoForMonth getMonthDataToDtoLimit2(Date date) {
        //상위 2개의 데이터만 추출
        List<SlotDtoForMonth> top2Slots = new ArrayList<>();
        if (date.getUser() == null && date.getGroup() != null) {
            top2Slots = date.getGroupSlot().stream()
                    .sorted(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed())
                    .limit(2)
                    .map(DateInGroupService::getGroupSlotInfoToDto)
                    .toList();
        } else if(date.getUser() != null && date.getGroup() == null) {
            top2Slots = date.getUserSlot().stream()
                    .sorted(Comparator.comparingInt((UserSlot s) -> s.getImportance().getPriority()).reversed())
                    .limit(2)
                    .map(DateInGroupService::getUserSlotInfoToDto)
                    .toList();
        }
        return DateDtoForMonth.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(
                        top2Slots
                )
                .build();
    }

    private static SlotDtoForMonth getGroupSlotInfoToDto(GroupSlot groupSlot) {

        return SlotDtoForMonth.builder()
                .groupId(groupSlot.getDate().getGroup().getId())
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }

    private static SlotDtoForMonth getUserSlotInfoToDto(UserSlot userSlot) {

        return SlotDtoForMonth.builder()
                .slotId(userSlot.getId())
                .title(userSlot.getTitle())
                .startTime(userSlot.getStartTime())
                .importance(userSlot.getImportance())
                .build();
    }
}