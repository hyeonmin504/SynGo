package backend.synGo.service;

import backend.synGo.controller.group.GroupSlotController;
import backend.synGo.domain.date.Date;
import backend.synGo.domain.group.Group;
import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.SlotMember;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static backend.synGo.form.responseForm.SlotResponseForm.*;
import static backend.synGo.service.SlotService.validDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupSlotService {
    private final UserGroupService userGroupService;
    private final UserGroupRepository userGroupRepository;
    private final DateService dateService;
    private final DateRepository dateRepository;
    private final GroupRepository groupRepository;
    private final GroupSlotRepository groupSlotRepository;

    @Transactional
    public Long createGroupSlot(Long groupId, SlotForm slotForm, Long userId) {
        checkUserGroupRole(groupId, userId);

        if (validDateTime(slotForm))
            throw new NotValidException("날자를 확인해주세요.");
        else if (slotForm.getEndDate() != null && slotForm.getStartDate().isEqual(slotForm.getEndDate()))
            slotForm.setEndDate(null);

        LocalDate startDate = slotForm.getStartDate().toLocalDate();
        log.info("startDate={}", startDate);
        Date date = dateRepository.findDateAndGroupSlotByStartDateAndUserId(startDate, groupId)   //fetch join
                .orElseGet(() -> {
                    Group group = groupRepository.findById(groupId).orElseThrow(() -> new NotFoundUserException("그룹 정보 없음"));
                    return new Date(group, startDate);
                });
        log.info("date={}", date);
        log.info("slotForm: {}", slotForm);
        //groupSlot 생성
        GroupSlot groupSlot = GroupSlot.createGroupSlot(
                slotForm.getStatus(),
                slotForm.getTitle(),
                slotForm.getContent(),
                slotForm.getStartDate(),
                slotForm.getEndDate(),
                slotForm.getPlace(),
                slotForm.getImportance(),
                date);
        //date의 SlotCount, summary를 업데이트
        dateService.updateGroupDateInfo(date, groupSlot);
        groupSlotRepository.save(groupSlot);
        return groupSlot.getId();
    }

    @Transactional(readOnly = true) //todo: SlotMember - userGroup 간 수정 필요
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
    public SlotIdResponse updateSlotData(Long groupId, Long slotId, Long userId, GroupSlotController.SlotUpdateForm form) {
        UserGroup requesterUserGroup = userGroupService.findUserGroupByUserIdAndGroupId(userId, groupId);
        if (requesterUserGroup.getRole().equals(Role.LEADER) || requesterUserGroup.getRole().equals(Role.MANAGER)) {
            GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                    .orElseThrow(() -> new NotFoundContentsException("슬롯 정보 없음"));
            GroupSlot updatedSlot = setGroupSlot(form, groupSlot, requesterUserGroup);
            return new SlotIdResponse(updatedSlot.getId());
        }
        throw new AccessDeniedException("권한 부족");
    }

    private static GroupSlot setGroupSlot(GroupSlotController.SlotUpdateForm form, GroupSlot groupSlot, UserGroup requesterUserGroup) {
        return groupSlot.updateSlot(requesterUserGroup.getNickname(), form.getTitle(), form.getContent(), form.getStartDate(), form.getEndDate(), form.getPlace(), form.getImportance());
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
}
