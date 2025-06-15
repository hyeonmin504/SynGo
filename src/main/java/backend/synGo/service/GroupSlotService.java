package backend.synGo.service;

import backend.synGo.config.scheduler.GroupSchedulerProvider;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.group.Group;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.SlotMember;
import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.form.responseForm.SlotResponseForm;
import backend.synGo.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static backend.synGo.controller.group.GroupSlotController.*;
import static backend.synGo.controller.group.SlotMemberController.*;
import static backend.synGo.form.responseForm.SlotResponseForm.*;
import static backend.synGo.service.SlotService.validDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupSlotService {
    private final UserGroupService userGroupService;
    private final UserGroupRepository userGroupRepository;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final GroupSlotRepository groupSlotRepository;
    private final GroupSchedulerProvider groupSchedulerProvider;

    @Transactional
    public Long createGroupSlot(Long groupId, SlotForm slotForm, Long userId) {
        checkUserGroupRole(groupId, userId);
        LocalDateTime updateDate = slotForm.getStartDate();

        if (validDateTime(slotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (slotForm.getEndDate() != null && updateDate.isEqual(slotForm.getEndDate()))
            slotForm.setEndDate(null);

        LocalDate startDate = updateDate.toLocalDate();
        log.info("startDate={}", startDate);
        Date date = dateRepository.findDateAndGroupSlotByStartDateAndUserId(startDate, groupId)   //fetch join
                .orElseGet(() -> {
                    Group group = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundUserException("그룹 정보 없음"));
                    return new Date(group, startDate);
                });
        //groupSlot 생성
        GroupSlot groupSlot = createGroupSlot(slotForm, date);
        //date의 SlotCount +1
        date.addSlotCount();
        groupSlotRepository.save(groupSlot);
        //캐시 초기화
        evictCache(groupId, userId, updateDate);
        return groupSlot.getId();
    }

    @Transactional(readOnly = true)
    public SlotResponseForm getGroupSlot(Long groupId, Long slotId, Long userId) {
        if (userGroupRepository.existsByGroupIdAndUserId(groupId, userId)){
            GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                    .orElseThrow(() -> new NotFoundContentsException("슬롯 정보 없음"));
            List<JoinMemberForm> joinMemberForm = buildSlotMember(groupSlot);
            return buildGroupSlotResponseForm(groupSlot, joinMemberForm);
        }
        throw new NotFoundUserException("그룹원 외 접근 불가");
    }

    @Transactional
    public SlotIdResponse updateSlotData(Long groupId, Long slotId, Long userId, SlotUpdateForm form) {
        UserGroup requesterUserGroup = userGroupService.findUserGroupByUserIdAndGroupId(userId, groupId);
        if (requesterUserGroup.getRole().equals(Role.LEADER) || requesterUserGroup.getRole().equals(Role.MANAGER)) {
            GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                    .orElseThrow(() -> new NotFoundContentsException("슬롯 정보 없음"));
            GroupSlot updatedSlot = setGroupSlot(form, groupSlot, requesterUserGroup);
            //캐시 초기화
            evictCache(groupId, userId, form.getStartDate());
            return new SlotIdResponse(updatedSlot.getId());
        }
        throw new AccessDeniedException("권한 부족");
    }

    @Transactional
    public SlotIdResponse updateSlotStatus(Long groupId, Long slotId, Long requesterUserId, GroupSlotStatusForm form) {
        //요청자의 유저그룹 조회
        UserGroup requesterUserGroup = userGroupRepository.findByGroupIdAndUserId(requesterUserId, groupId)
                .orElseThrow(() -> new NotFoundUserException("그룹에 속한 유저가 아닙니다"));
        //현재 슬롯 기준 slotMember, UserGroup 페치 조인
        GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                .orElseThrow(() -> new NotFoundContentsException("슬롯이 존재하지 않습니다"));
        //슬롯에서 시작해 모든 슬롯맴버들의 userGroupId 중에 요청자의 userGroup.id와 매칭되는 값을 찾음
        Optional<SlotMember> requesterSlotMember = groupSlot.getSlotMember().stream().filter(sm -> sm.getUserGroup().getId().equals(requesterUserGroup.getId()))
                .findFirst();
        if ( requesterUserGroup.getRole().equals(Role.LEADER) ){
            log.info("리더의 슬롯 생태 변경 요청");
            groupSlot.updateStatus(form.getStatus(), requesterUserGroup.getNickname());
            return new SlotIdResponse(slotId);
        }
        if ( requesterSlotMember.isPresent() && requesterSlotMember.get().getSlotPermission().equals(SlotPermission.EDITOR )) {
            log.info("에디터의 슬롯 생태 변경 요청");
            groupSlot.updateStatus(form.getStatus(), requesterUserGroup.getNickname());
            return new SlotIdResponse(slotId);
        }
        throw new AccessDeniedException("변경 권한이 없습니다");
    }

    private void evictCache(Long groupId, Long userId, LocalDateTime updateDate) {
        if (groupSchedulerProvider.isSameYearAndMonth(updateDate.toLocalDate())){
            //이번 달에 슬롯 추가 시 캐시 초기화
            groupSchedulerProvider.evictGroupSchedule(groupId, updateDate.getYear(), updateDate.getMonthValue());
            groupSchedulerProvider.evictMyGroupSchedule(userId, updateDate.getYear(), updateDate.getMonthValue());
        } else if (groupSchedulerProvider.isSameYearAndMonthPlusOne(updateDate.toLocalDate())) {
            //다음 달에 슬롯 추가 시 그룹 캐시 초기화
            groupSchedulerProvider.evictGroupSchedule(groupId, updateDate.getYear(), updateDate.getMonthValue());
        }
    }

    private List<JoinMemberForm> buildSlotMember(GroupSlot groupSlot) {
        List<SlotMember> members = groupSlot.getSlotMember();
        if (members.isEmpty()) return new ArrayList<>();
        return members.stream().map(member -> JoinMemberForm
                .builder()
                .joinMemberId(member.getId())
                .nickname(member.getUserGroup().getNickname())
                .permission(member.getSlotPermission().getSlotPermission())
                .build()).collect(Collectors.toList());
    }

    private static SlotResponseForm buildGroupSlotResponseForm(GroupSlot groupSlot, List<JoinMemberForm> joinMemberForm) {

        return builder()
                .slotId(groupSlot.getId())
                .title(groupSlot.getTitle())
                .content(groupSlot.getContent())
                .startDate(groupSlot.getStartTime())
                .endDate(groupSlot.getEndTime())
                .createDate(groupSlot.getCreateDate())
                .place(groupSlot.getPlace())
                .importance(groupSlot.getImportance().getLabel())
                .status(groupSlot.getStatus().getStatus())
                .updater(groupSlot.getUpdateUser())
                .member(joinMemberForm)
                .build();
    }

    private void checkUserGroupRole(Long groupId, Long userId) {
        UserGroup requesterUserGroup = userGroupService.findUserGroupByUserIdAndGroupId(userId, groupId);
        if (requesterUserGroup.getRole() != Role.LEADER && requesterUserGroup.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("권한 부족");
        }
    }

    private static GroupSlot setGroupSlot(SlotUpdateForm form, GroupSlot groupSlot, UserGroup requesterUserGroup) {
        return groupSlot.updateSlot(requesterUserGroup.getNickname(), form.getTitle(), form.getContent(), form.getStartDate(), form.getEndDate(), form.getPlace(), form.getImportance());
    }

    private static GroupSlot createGroupSlot(SlotForm slotForm, Date date) {
        return GroupSlot.createGroupSlot(
                slotForm.getStatus(),
                slotForm.getTitle(),
                slotForm.getContent(),
                slotForm.getStartDate(),
                slotForm.getEndDate(),
                slotForm.getPlace(),
                slotForm.getImportance(),
                date);
    }
//    //쿼리 두번에 유지보수 강함
//    private DateDtoForDay findAllDateInfoTwoQuery(Date date) {
//        // 1차 쿼리: slot 기본 정보만 조회
//        List<SlotDtoForDay> baseSlotDtos = groupSlotRepository.findDateAndSlotByGroupIdAndDay(date.getId());
//
//        // slotId 추출
//        List<Long> slotIds = baseSlotDtos.stream()
//                .map(SlotDtoForDay::getSlotId)
//                .toList();
//
//        // Map 형태로 매핑
//        Map<Long, SlotDtoForDay> slotMap = baseSlotDtos.stream()
//                .collect(Collectors.toMap(SlotDtoForDay::getSlotId, dto -> dto));
//
//        // 2차 쿼리: slotId에 해당하는 editor 정보만 조회
//        List<SlotDtoForDay> editorDtos = groupSlotRepository.findMemberWithUserGroupBySlotIdsAndLeader(slotIds);
//
//        // 필요한 필드만 업데이트 (editor 정보)
//        for (SlotDtoForDay editorDto : editorDtos) {
//            SlotDtoForDay original = slotMap.get(editorDto.getSlotId());
//            if (original != null) {
//                original.setUserGroupId(editorDto.getUserGroupId());
//                original.setEditorNickname(editorDto.getEditorNickname());
//            }
//        }
//
//        return DateDtoForDay.builder()
//                .slotCount(date.getSlotCount())
//                .today(date.getStartDate())
//                .slotDtoForDay(baseSlotDtos)
//                .build();
//    }
}
