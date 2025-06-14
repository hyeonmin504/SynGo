package backend.synGo.service.date.user;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.form.DaySlotDto;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.GroupSlotRepository;
import backend.synGo.repository.UserGroupRepository;
import backend.synGo.repository.UserSlotRepository;
import backend.synGo.service.date.group.DateInGroupService;
import jakarta.annotation.Nullable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

import static backend.synGo.controller.date.UserDataDateSearchController.*;
import static backend.synGo.service.GroupSlotService.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateUserService {

    private final UserGroupRepository userGroupRepository;
    private final UserSlotRepository userSlotRepository;
    private final DateRepository dateRepository;
    private final GroupSlotRepository groupSlotRepository;

    /**
     * 유저의 개인 데이터 한달 단위로 가져오는 서비스
     * @param year
     * @param month
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public List<MonthDateInfoGroupVer> getUserDataDatesForMonth(int year, int month, Long requestUserId) {
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //Redis 캐시 조회
//        Optional<GroupDateInfo> cachedSchedule = schedulerProvider.getGroupSchedule(requesterUserId, year, month);
        //캐시 존재 && 이번 달 인 경우
//        if ((isCurrentMonth) && cachedSchedule.isPresent()) {
//            log.info("캐시 조회중");
//            return cachedSchedule.get();
//        }
        //DB 조회
        List<Date> dateByMonth = findUserDataByMonth(year, month, requestUserId);
        //dto 작성
        List<MonthDateInfoGroupVer> monthDateDto = dateByMonth.stream()
                .map(DateInGroupService::getDateInfoToDto)
                .toList();

//        if (isCurrentMonth) {
//            schedulerProvider.saveGroupScheduler(groupId, groupDateInfo, year, month);
//            log.info("데이터 캐싱");
//            return groupDateInfo;
//        }
        return monthDateDto;
    }

    /**
     * 유저의 개인 데이터 한달 단위로 db 요청
     * @param year
     * @param month
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    private List<Date> findUserDataByMonth(int year, int month, Long requestUserId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        // startDate ~ endDate 전날 까지 Date 모두 조회
        return dateRepository.findAllUserDataByMonthRange(requestUserId, startDate, endDate);
    }

    /**
     * 유저의 그룹 내 데이터 한달 단위로 가져오는 서비스
     * @param year
     * @param month
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public List<MonthDateInfoGroupVer> getUserDataDatesForMonthByGroup(int year, int month, Long requestUserId) {
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //Redis 캐시 조회
//        Optional<GroupDateInfo> cachedSchedule = schedulerProvider.getGroupSchedule(requesterUserId, year, month);
        //캐시 존재 && 이번 달 인 경우
//        if ((isCurrentMonth) && cachedSchedule.isPresent()) {
//            log.info("캐시 조회중");
//            return cachedSchedule.get();
//        }
        //DB 조회
        List<Date> dateByMonth = findUserDataForMonthByGroup(year, month, requestUserId);
        //dto 작성
        List<MonthDateInfoGroupVer> monthDateInfo = dateByMonth.stream()
                .map(DateUserService::getDateInfoToDtoGroupVer)
                .toList();
        //dto 정렬 -> date의 today로 정렬
        List<MonthDateInfoGroupVer> monthArrayDateInfo = monthDateInfo.stream()
                .collect(Collectors.groupingBy(
                        MonthDateInfoGroupVer::getToday,
                        // 초기값은 빈 MonthDateInfoGroupVer
                        // 스트림에서 받은 값 m은 따로 가공하지 않고 그대로 사용 (m -> m)
                        // 동일한 날짜(today)에 해당하는 값들끼리 mergeDateInfos()로 병합
                        Collectors.reducing(
                                new MonthDateInfoGroupVer(0, null, new ArrayList<>()),
                                m -> m,
                                DateUserService::mergeDateInfos
                        )
                ))
                .values()
                .stream()
                .toList();
        //오늘 인 경우 캐싱
//        if (isCurrentMonth) {
//            schedulerProvider.saveGroupScheduler(groupId, groupDateInfo, year, month);
//            log.info("데이터 캐싱");
//            return groupDateInfo;
//        }

        return monthArrayDateInfo;
    }

    private static MonthDateInfoGroupVer getDateInfoToDtoGroupVer(Date date) {
        //각 그룹의 date 당 상위 2개의 데이터만 추출 및 dto 작성
        List<SlotInfoByMonth> top2Slots = date.getGroupSlot().stream()
                .sorted(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed())
                .limit(2)
                .map(DateUserService::getUserSlotInfoToDtoForGroup)
                .toList();

        return MonthDateInfoGroupVer.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(top2Slots)
                .build();
    }

    private static SlotInfoByMonth getUserSlotInfoToDtoForGroup(GroupSlot groupSlot) {
        return SlotInfoByMonth.builder()
                .groupId(groupSlot.getDate().getGroup().getId())
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }

    /**
     * Date, groupSlot db요청 후 mapping 및 유의미 한 데이터 추출
     * @param year
     * @param month
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    private List<Date> findUserDataForMonthByGroup(int year, int month, Long requestUserId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        List<UserGroup> userGroups = userGroupRepository.findUserDataByUserId(requestUserId, startDate, endDate);
        if (userGroups.isEmpty()) return new ArrayList<>();

        // 2. Date 전체 수집
        List<Date> allDates = userGroups.stream()
                .flatMap(ug -> ug.getGroup().getDate().stream())
                .toList();
        // 3. dateId 수집
        List<Long> dateIds = allDates.stream()
                .map(Date::getId)
                .toList();
        // 4. GroupSlot 일괄 조회, batch fetch size 를 통해 데이터 추출
        List<GroupSlot> groupSlots = groupSlotRepository.findByDateIdIn(dateIds);
        // 5. dateId → GroupSlot 매핑
        Map<Long, List<GroupSlot>> slotMap = groupSlots.stream()
                .collect(Collectors.groupingBy(gs -> gs.getDate().getId()));
        // 6. 각 Date에 매핑된 slot 세팅
        allDates.forEach(date -> {
            List<GroupSlot> slots = slotMap.getOrDefault(date.getId(), Collections.emptyList());
            date.setGroupSlot(slots); // 혹은 DTO 조립
        });
        // 7. 빈 값 제외
        return allDates.stream()
                .filter(date -> {
                    List<GroupSlot> slots = slotMap.get(date.getId());
                    return slots != null && !slots.isEmpty();
                })
                .toList();
    }

    /**
     * date의 today 정렬 병합 메서드
     * @param m1
     * @param m2
     * @return
     */
    private static MonthDateInfoGroupVer mergeDateInfos(MonthDateInfoGroupVer m1, MonthDateInfoGroupVer m2) {
        int totalSlotCount = m1.getSlotCount() + m2.getSlotCount();
        LocalDate today = m1.getToday() != null ? m1.getToday() : m2.getToday();

        List<SlotInfoByMonth> mergedSlots = new ArrayList<>();
        mergedSlots.addAll(m1.getSlotInfo());
        mergedSlots.addAll(m2.getSlotInfo());

        // 중요도 순 정렬 후 상위 2개 선택
        List<SlotInfoByMonth> top2Slots = mergedSlots.stream()
                .sorted(Comparator.comparingInt((SlotInfoByMonth s) -> s.getImportance().getPriority()).reversed())
                .limit(2)
                .toList();

        return MonthDateInfoGroupVer.builder()
                .slotCount(totalSlotCount)
                .today(today)
                .slotInfo(top2Slots)
                .build();
    }

     /**
     * 유저의 개인 데이터 하루 단위로 가져오는 서비스
     * @param year
     * @param month
     * @param day
     * @param requestUserId
     */
    public DateInfo getUserDataDatesForDayByGroup(int year, int month, int day, Long requestUserId) {
        //요청한 날자
        LocalDate localDate = LocalDate.of(year,month,day);
        //선택한 date 조회
        Optional<Date> optionalDate = dateRepository.findUserDateByDay(requestUserId, localDate);

        if (optionalDate.isPresent()) {
            log.info("optionalDate.isPresent()");
            return findAllUserDate(optionalDate.get());
        }
        return new DateInfo();
    }

    @Transactional(readOnly = true)
    private DateInfo findAllUserDate(Date date) {
        List<UserSlot> slots = date.getUserSlot();
        return DateInfo.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .daySlotDtos(slots.stream().map(userSlot -> DaySlotDto.builder()
                        .slotId(userSlot.getId())
                        .title(userSlot.getTitle())
                        .startTime(userSlot.getStartTime())
                        .endTime(userSlot.getEndTime())
                        .importance(userSlot.getImportance())
                        .userGroupId(null)
                        .editorNickname(null)
                        .build()).toList())
                .build();
    }

    Long slotId;
    String title;
    LocalDateTime startTime;
    LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    SlotImportance importance;
    @Nullable
    Long userGroupId;
    @Nullable
    String editorNickname;
}
