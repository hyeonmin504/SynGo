package backend.synGo.service;

import backend.synGo.config.scheduler.SchedulerProvider;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.repository.DateRepository;
import backend.synGo.repository.GroupSlotRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static backend.synGo.controller.date.GroupDateSearchController.*;
import static backend.synGo.controller.date.UserDataDateSearchController.*;
import static backend.synGo.service.GroupSlotService.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DateService {

    private final SchedulerProvider schedulerProvider;
    private final UserGroupRepository userGroupRepository;
    private final DateRepository dateRepository;
    private final GroupSlotService groupSlotService;
    private final GroupSlotRepository groupSlotRepository;

    @Transactional(readOnly = true)
    public GroupDateInfo getDatesForMonthInGroup(Long groupId, int year, int month, Long requesterUserId) {
        //인증 조회
        if(!userGroupRepository.existsByGroupIdAndUserId(groupId,requesterUserId)) {
            throw new AccessDeniedException("그룹원 외 접근 불가");
        }
        //이번 달을 조회 한 경우
        boolean isCurrentMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue();
        //다음 달을 조회 한 경우
        boolean isNextMonth = year == LocalDate.now().getYear() && month == LocalDate.now().getMonthValue()+1;
        //Redis 캐시 조회
        Optional<GroupDateInfo> cachedSchedule = schedulerProvider.getGroupSchedule(groupId, year, month);
        //캐시 존재 && 이번 달 혹은 다음 달 인 경우
        if ((isCurrentMonth || isNextMonth) && cachedSchedule.isPresent()) {
            log.info("캐시 조회중");
            return cachedSchedule.get();
        }
        //DB 조회
        List<Date> dateByMonth = findGroupDataByMonth(year, month, groupId);
        //dto 작성
        List<MonthDateInfo> monthDateInfo = dateByMonth.stream()
                .map(DateService::getDateInfoToDto)
                .toList();

        GroupDateInfo groupDateInfo = new GroupDateInfo(groupId, monthDateInfo);
        if (isCurrentMonth || isNextMonth) {
            schedulerProvider.saveGroupScheduler(groupId, groupDateInfo, year, month);
            log.info("데이터 캐싱");
            return groupDateInfo;
        }
        return groupDateInfo;
    }

    @Transactional(readOnly = true)
    private List<Date> findGroupDataByMonth(int year, int month, Long groupId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        // startDate ~ endDate 전날 까지 Date 모두 조회
        return dateRepository.findScheduleDateWithSlotsByGroupAndMonthRange(groupId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public DateInfo findGroupDataByDayInGroup(Long groupId, int year, int month, int day, Long requestUserId) {
        if(userGroupRepository.existsByGroupIdAndUserId(groupId,requestUserId)) {
            return groupSlotService.findDateByDay(groupId, year, month, day);
        }
        throw new AccessDeniedException("그룹원 외 접근 불가");
    }

    @Transactional(readOnly = true)
    public List<MonthDateInfo> getUserDataDatesForMonth(int year, int month, Long requestUserId) {
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
        List<MonthDateInfo> monthDateInfo = dateByMonth.stream()
                .map(DateService::getDateInfoToDto)
                .toList();

//        if (isCurrentMonth) {
//            schedulerProvider.saveGroupScheduler(groupId, groupDateInfo, year, month);
//            log.info("데이터 캐싱");
//            return groupDateInfo;
//        }
        return monthDateInfo;
    }

    @Transactional(readOnly = true)
    private List<Date> findUserDataByMonth(int year, int month, Long requestUserId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.plusMonths(1).atDay(1); // 해당 월의 마지막 날

        // startDate ~ endDate 전날 까지 Date 모두 조회
        return dateRepository.findAllUserDataByMonthRange(requestUserId, startDate, endDate);
    }

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
                .map(DateService::getDateInfoToDtoGroupVer)
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
                                DateService::mergeDateInfos
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

    // date의 today 정렬 병합 메서드
    private static MonthDateInfoGroupVer mergeDateInfos(MonthDateInfoGroupVer m1, MonthDateInfoGroupVer m2) {
        int totalSlotCount = m1.getSlotCount() + m2.getSlotCount();
        LocalDate today = m1.getToday() != null ? m1.getToday() : m2.getToday();

        List<SlotInfoContainGroupId> mergedSlots = new ArrayList<>();
        mergedSlots.addAll(m1.getSlotInfo());
        mergedSlots.addAll(m2.getSlotInfo());

        // 중요도 순 정렬 후 상위 2개 선택
        List<SlotInfoContainGroupId> top2Slots = mergedSlots.stream()
                .sorted(Comparator.comparingInt((SlotInfoContainGroupId s) -> s.getImportance().getPriority()).reversed())
                .limit(2)
                .toList();

        return MonthDateInfoGroupVer.builder()
                .slotCount(totalSlotCount)
                .today(today)
                .slotInfo(top2Slots)
                .build();
    }

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
        // 4. GroupSlot 일괄 조회
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

    private static MonthDateInfo getDateInfoToDto(Date date) {
        //상위 2개의 데이터만 추출
        List<SlotInfo> top2Slots = new ArrayList<>();
        if (date.getUser() == null && date.getGroup() != null) {
            top2Slots = date.getGroupSlot().stream()
                    .sorted(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed())
                    .limit(2)
                    .map(DateService::getGroupSlotInfoToDto)
                    .toList();
        } else if(date.getUser() != null && date.getGroup() == null) {
            top2Slots = date.getUserSlot().stream()
                    .sorted(Comparator.comparingInt((UserSlot s) -> s.getImportance().getPriority()).reversed())
                    .limit(2)
                    .map(DateService::getUserSlotInfoToDto)
                    .toList();
        }
        return MonthDateInfo.builder()
                .dateId(date.getId())
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(
                        top2Slots
                )
                .build();
    }

    private static MonthDateInfoGroupVer getDateInfoToDtoGroupVer(Date date) {
        //각 그룹의 date 당 상위 2개의 데이터만 추출 및 dto 작성
        List<SlotInfoContainGroupId> top2Slots = date.getGroupSlot().stream()
                .sorted(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed())
                .limit(2)
                .map(DateService::getUserSlotInfoToDtoForGroup)
                .toList();

        return MonthDateInfoGroupVer.builder()
                .slotCount(date.getSlotCount())
                .today(date.getStartDate())
                .slotInfo(top2Slots)
                .build();
    }

    private static SlotInfoContainGroupId getUserSlotInfoToDtoForGroup(GroupSlot groupSlot) {
        return SlotInfoContainGroupId.builder()
                .groupId(groupSlot.getDate().getGroup().getId())
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }

    private static SlotInfo getGroupSlotInfoToDto(GroupSlot groupSlot) {

        return SlotInfo.builder()
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .startTime(groupSlot.getStartTime())
                .importance(groupSlot.getImportance())
                .build();
    }

    private static SlotInfo getUserSlotInfoToDto(UserSlot userSlot) {

        return SlotInfo.builder()
                .slotId(userSlot.getId())
                .title(userSlot.getTitle())
                .startTime(userSlot.getStartTime())
                .importance(userSlot.getImportance())
                .build();
    }


//    /**
//     * user이 생성한 date의 SlotCount, summary를 업데이트 todo: 그룹 슬롯과 유저 슬롯을 통합해서 summary를 결정하기
//     * @param date
//     * @param userSlot
//     * @return
//     */
//    @Deprecated // TODO: 6/5/25 user,group 두개 슬롯 포함한 업데이트로 변경
//    public void updateDateInfo(Date date, UserSlot userSlot) {
//        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
//        if (date.getSlotCount() != 0){
//            List<UserSlot> allSlot = date.getUserSlot();
//            allSlot.add(userSlot);
//            //중요도로 내림차순 정렬한다
//            allSlot.sort(Comparator.comparingInt((UserSlot s) -> s.getImportance().getPriority()).reversed());
//
//            //최신화 문제를 예방하기 위해 안전하게 접근
//            String summary = allSlot.stream()
//                    .limit(2)
//                    .map(UserSlot::getTitle)
//                    .collect(Collectors.joining(","));
//
//            date.addSlotCountAndSummary(summary);
//        }
//        else
//            date.addSlotCountAndSummary(userSlot.getTitle());

//    }
//    /**
//     * user이 생성한 date의 SlotCount, summary를 업데이트
//     * @param date
//     * @param groupSlot
//     * @return
//     */
//    @Deprecated // TODO: 6/5/25  이건 date 조회 시 설정 하도록 위치 변경 예정
//    public void updateGroupDateInfo(Date date, GroupSlot groupSlot) {
//        //첫 슬롯 생성이 아닌 경우 새로 생성한 슬롯과 더불어 중요도가 높은 2개의 슬롯을 뽑아 date의 요약에 넣는다
//        if (date.getSlotCount() != 0){
//            log.info("date.getSlotCount()={}",date.getSlotCount());
//            List<GroupSlot> allSlot = date.getGroupSlot();
//            allSlot.add(groupSlot);
//            //중요도로 내림차순 정렬한다
//            allSlot.sort(Comparator.comparingInt((GroupSlot s) -> s.getImportance().getPriority()).reversed());
//
//            //최신화 문제를 예방하기 위해 안전하게 접근
//            String summary = allSlot.stream()
//                    .limit(2)
//                    .map(GroupSlot::getTitle)
//                    .collect(Collectors.joining(","));
//
//            date.addSlotCountAndSummary(summary);
//        }
//        else
//            date.addSlotCountAndSummary(groupSlot.getTitle());
//    }
}