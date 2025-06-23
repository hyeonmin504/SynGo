package backend.synGo.service;

import backend.synGo.domain.slot.GroupSlot;
import backend.synGo.domain.slot.SlotMember;
import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.repository.GroupSlotRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static backend.synGo.controller.group.SlotMemberController.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlotMemberService {

    private final GroupSlotRepository groupSlotRepository;
    private final UserGroupService userGroupService;
    private final UserGroupRepository userGroupRepository;
    private final SlotPermissionService permissionService;
    private final GroupSlotService groupSlotService;

    @Transactional
    public SlotIdResponse registerGroupSlotMember(Long groupId, Long userId, Long slotId, List<JoinMemberRequestForm> form) {
        GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                .orElseThrow(() -> new NotFoundContentsException("슬롯이 존재하지 않습니다"));

        List<UserGroup> userGroups = userGroupRepository.joinUserGroupAndGroupFindAllUserGroupByGroupId(groupId);
        if (!userGroupService.checkUserInGroup(userId, userGroups)) {
            throw new AccessDeniedException("그룹원 외 접근 불가");
        }
        // 현재 슬롯의 slotMember 중에 EDITOR가 존재하는지 확인
        boolean hasEditor = groupSlot.getSlotMember().stream()
                .anyMatch(member -> member.getSlotPermission().getSlotPermission().equals(SlotPermission.EDITOR));
        log.info("에디터 존재 유무={}", hasEditor);
        // userGroupId -> UserGroup 매핑
        Map<Long, UserGroup> userGroupMap = userGroups.stream()
                .collect(Collectors.toMap(UserGroup::getId, ug -> ug));
        log.info("request.getPermission={}", form.get(0).getPermission());
        for (JoinMemberRequestForm request : form) {
            log.info("request.getPermission={}", request.getPermission());
            UserGroup targetUG = userGroupMap.get(request.getUserGroupId());
            if (targetUG == null) {
                throw new NotFoundContentsException("userGroupId에 해당하는 그룹원이 없습니다: " + request.getUserGroupId());
            }

            // 이미 존재하는 SlotMember인지 확인
            if (isAlreadyRegistered(request, groupSlot)) {
                log.info("이미 유저가 존재={}", targetUG.getNickname());
                continue; // 이미 등록된 경우 무시
            }
            log.info("request.getPermission={}", request.getPermission());
            if (Objects.equals(request.getPermission(), SlotPermission.EDITOR) && hasEditor) {
                throw new NotValidException("이미 EDITOR 권한을 가진 멤버가 존재합니다.");
            }
            // 새 SlotMember 생성 및 연관관계 설정 (연관 메서드 내부에서 add 연결)
            SlotMember member = new SlotMember(permissionService.getSlotPermission(request.getPermission()), targetUG, groupSlot);
            log.info("member.getSlotPermission={}",member.getSlotPermission());
            // EDITOR 권한 설정되면 플래그 true로 변경
            if (request.getPermission().equals(SlotPermission.EDITOR)) {
                hasEditor = true;
                //websocket 메시지 발행
                groupSlotService.groupMemberSyncGoPub(groupId ,groupSlot.getStartTime().toLocalDate() ,slotId);
            }
        }
        // SlotMember는 Cascade로 저장됨
        groupSlotRepository.save(groupSlot);
        if (!hasEditor) {
            // 만약 EDITOR가 없다면 websocket 슬롯 업데이트 메시지 발행
            groupSlotService.groupSlotSyncGoPub(groupId, slotId);
        }
        return new SlotIdResponse(groupSlot.getId());
    }

    @Transactional(readOnly = true)
    public List<JoinMemberRequestForm> getGroupSlotMember(Long groupId, Long requesterUserId, Long slotId) {
        if (userGroupRepository.existsByGroupIdAndUserId(groupId,requesterUserId)){
            GroupSlot groupSlot = groupSlotRepository.joinSlotMemberAndUserGroupBySlotId(slotId)
                    .orElseThrow(() -> new NotFoundContentsException("슬롯이 존재하지 않습니다"));
            return groupSlot.getSlotMember().stream()
                    .map(sm -> new JoinMemberRequestForm(sm.getUserGroup().getId(), sm.getUserGroup().getNickname(), sm.getSlotPermission().getSlotPermission()))
                    .collect(Collectors.toList());
        }
        throw new AccessDeniedException("그룹원 외 접근 불가");
    }

    private static boolean isAlreadyRegistered(JoinMemberRequestForm request, GroupSlot groupSlot) {
        return groupSlot.getSlotMember().stream()
                .anyMatch(member -> member.getUserGroup().getId().equals(request.getUserGroupId()));
    }
}
