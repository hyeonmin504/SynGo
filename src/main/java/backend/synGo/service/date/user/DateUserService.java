package backend.synGo.service.date.user;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.form.DateDtoForDay;
import backend.synGo.form.DateDtoForMonth;
import backend.synGo.form.SlotDtoForDay;
import backend.synGo.form.SlotDtoForMonth;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.GroupSlotRepository;
import backend.synGo.repository.UserGroupRepository;
import backend.synGo.repository.UserSlotRepository;
import backend.synGo.service.date.group.DateInGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

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
    public List<DateDtoForMonth> getUserDataDatesForMonth(int year, int month, Long requestUserId) {
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
        List<DateDtoForMonth> monthDateDto = dateByMonth.stream()
                .map(DateInGroupService::getMonthDataToDtoLimit2)
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
     * 유저의 그룹 데이터 한달 단위로 요청
     * @param year
     * @param month
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public List<DateDtoForMonth> getUserDataDatesForMonthByGroup(int year, int month, Long requestUserId) {
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //Redis 캐시 조회
//        Optional<GroupDateInfo> cachedSchedule = schedulerProvider.getGroupSchedule(requesterUserId, year, month);
        //캐시 존재 && 이번 달 인 경우
//        if ((isCurrentMonth) && cachedSchedule.isPresent()) {
//            log.info("캐시 조회중");
//            return cachedSchedule.get();
//        }
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날
        //userGroup -> group -> date 까지 조회
        List<UserGroup> userGroups = userGroupRepository.findUserGroupDataByUserIdForMonth(requestUserId, startDate, endDate);
        if (userGroups.isEmpty()) return new ArrayList<>();
        //date -> groupSlot 조회 및 데이터 추출
        List<Date> dateByMonth = findUserDataByGroup(userGroups);
        //dto 작성
        List<DateDtoForMonth> monthDateInfo = dateByMonth.stream()
                .map(DateUserService::getDateInfoToDtoGroupVer)
                .toList();
        //dto 정렬 -> date의 today로 정렬
        List<DateDtoForMonth> monthArrayDateInfo = sortMonthDateInfoGroupVerForToday(monthDateInfo);
        //오늘 인 경우 캐싱
//        if (isCurrentMonth) {
//            schedulerProvider.saveGroupScheduler(groupId, groupDateInfo, year, month);
//            log.info("데이터 캐싱");
//            return groupDateInfo;
//        }
        return monthArrayDateInfo;
    }

    /**
     * dto 정렬 -> date의 today로 정렬
     * @param monthDateInfo
     * @return
     */
    private static List<DateDtoForMonth> sortMonthDateInfoGroupVerForToday(List<DateDtoForMonth> monthDateInfo) {
        return monthDateInfo.stream()
                .collect(Collectors.groupingBy(
                        DateDtoForMonth::getToday,
                        // 초기값은 빈 MonthDateInfoGroupVer
                        // 스트림에서 받은 값 m은 따로 가공하지 않고 그대로 사용 (m -> m)
                        // 동일한 날짜(today)에 해당하는 값들끼리 mergeDateInfos()로 병합
                        Collectors.reducing(
                                new DateDtoForMonth(0, null, new ArrayList<>()),
                                m -> m,
                                DateUserService::mergeDateInfos
                        )
                ))
                .values()
                .stream()
                .toList();
    }

    /**
     * date의 한달간 today 정렬 병합 메서드
     * @param m1
     * @param m2
     * @return
     */
    private static DateDtoForMonth mergeDateInfos(DateDtoForMonth m1, DateDtoForMonth m2) {
        int totalSlotCount = m1.getSlotCount() + m2.getSlotCount();
        LocalDate today = m1.getToday() != null ? m1.getToday() : m2.getToday();

        List<SlotDtoForMonth> mergedSlots = new ArrayList<>();
        mergedSlots.addAll(m1.getSlotInfo());
        mergedSlots.addAll(m2.getSlotInfo());

        // 중요도 순 정렬 후 상위 2개 선택
        List<SlotDtoForMonth> top2Slots = mergedSlots.stream()
                .sorted(Comparator.comparingInt((SlotDtoForMonth s) -> s.getImportance().getPriority()).reversed())
                .limit(2)
                .toList();

        return DateDtoForMonth.builder()
                .slotCount(totalSlotCount)
                .today(today)
                .slotInfo(top2Slots)
                .build();
    }

    /**
     * 유저의 그룹내 해당 date에 반환을 위 dto 각 변환
     * @param date
     * @return
     */
    private static DateDtoForMonth getDateInfoToDtoGroupVer(Date date) {
        List<SlotDtoForMonth> top2Slots = date.getGroupSlot().stream()
                .map(DateUserService::getUserSlotInfoToDtoForGroup)
                .toList();

        return DateDtoForMonth.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(top2Slots)
                .build();
    }

    private static SlotDtoForMonth getUserSlotInfoToDtoForGroup(GroupSlot groupSlot) {
        return SlotDtoForMonth.builder()
                .groupId(groupSlot.getDate().getGroup().getId())
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }

    /**
     * 유저의 정보를 바탕으로 Date -> groupSlot db요청 후 mapping 및 유의미 한 데이터 추출
     * @param userGroups
     * @return
     */
    @Transactional(readOnly = true)
    private List<Date> findUserDataByGroup(List<UserGroup> userGroups) {
        List<Date> allDates = userGroups.stream()
                .flatMap(ug -> ug.getGroup().getDate().stream())
                .toList();
        if (allDates.isEmpty()) return List.of();
        Map<Long, Date> dateMap = allDates.stream()
                .collect(Collectors.toMap(Date::getId, d -> d));
        List<GroupSlot> groupSlots = groupSlotRepository.findByDateIdIn(dateMap.keySet().stream().toList());
        // slot을 해당 Date에 할당
        groupSlots.forEach(slot -> {
            Date date = dateMap.get(slot.getDate().getId());
            if (date != null) {
                date.getGroupSlot().add(slot);
            }
        });
        // groupSlot이 비어있지 않은 date만 반환
        return dateMap.values().stream()
                .filter(d -> !d.getGroupSlot().isEmpty())
                .toList();
    }

     /**
     * 유저의 개인 데이터 하루 단위로 가져오는 서비스
     * @param year
     * @param month
     * @param day
     * @param requestUserId
     */
     @Transactional(readOnly = true)
    public DateDtoForDay getUserDataDatesForDay(int year, int month, int day, Long requestUserId) {
        //요청한 날자
        LocalDate localDate = LocalDate.of(year,month,day);
        //선택한 date 조회
        Optional<Date> optionalDate = dateRepository.findUserDateByDay(requestUserId, localDate);

        if (optionalDate.isPresent()) {
            log.info("optionalDate.isPresent()");
            return findAllUserDate(optionalDate.get());
        }
        return new DateDtoForDay();
    }

    @Transactional(readOnly = true)
    private DateDtoForDay findAllUserDate(Date date) {
        List<UserSlot> slots = date.getUserSlot();
        return DateDtoForDay.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(slots.stream().map(userSlot -> SlotDtoForDay.builder()
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

    /**
     * 유저와 연관된 그룹의 하루 개인 데이터 조회
     * @param year
     * @param month
     * @param day
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public DateDtoForDay getUserDataDatesForDayByGroup(int year, int month, int day, Long requestUserId) {
        //선택한 date 조회
        LocalDate localDate = LocalDate.of(year, month, day);
        //userGroup -> Group -> date 조회
        List<UserGroup> userGroups = userGroupRepository.findUserDataByUserIdForDay(requestUserId, localDate);
        if (userGroups.isEmpty()) userGroups = new ArrayList<>();
        //dateIds -> groupSlot 조회 및 데이터 추출
        List<SlotDtoForDay> groupSlots = findGroupSlotsByUserGroups(userGroups);
        //dto 작성
        return DateDtoForDay.builder()
                .slotCount(groupSlots.size())
                .today(localDate)
                .slotInfo(groupSlots)
                .build();
    }

    /**
     *
     * @param userGroups
     * @return
     */
    @Transactional(readOnly = true)
    private List<SlotDtoForDay> findGroupSlotsByUserGroups(List<UserGroup> userGroups) {
        // 1. 유저가 속한 그룹들의 날짜 ID 수집
        List<Long> dateIds = userGroups.stream()
                .flatMap(ug -> ug.getGroup().getDate().stream())
                .map(Date::getId)
                .toList();
        // 2. 날짜가 없는 경우 빈 리스트 반환
        if (dateIds.isEmpty()) return Collections.emptyList();
        // 3. 날짜 ID 기준으로 GroupSlot과 관련된 데이터 조회
        return groupSlotRepository.findWithMemberAndUserGroupByDateIdIn(dateIds);
    }
}
