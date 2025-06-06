package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.*;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import backend.synGo.form.responseForm.GroupIdResponseForm;
import backend.synGo.repository.GroupRepository;
import backend.synGo.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static backend.synGo.controller.group.GroupBasicController.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserGroupService userGroupService;
    private final GroupSchedulerService groupSchedulerService;
    private final UserGroupRepository userGroupRepository;

    /**
     * 그룹 생성 -> userGroup, GroupScheduler 생성
     * @param requestForm
     * @param userId
     * @return
     */
    @Transactional
    public Long createGroupAndReturnUserGroupId(GroupRequestForm requestForm, Long userId) {
        //group 체크및 생성
        Group group = buildGroupFromRequest(requestForm);
        //group 저장
        Group savedGroup = groupRepository.save(group);
        //UserGroup 생성및 저장
        userGroupService.saveUserGroupData(requestForm.getNickname(), userService.findUserById(userId), savedGroup, Role.LEADER);
        //groupScheduler 생성및 저장
        groupSchedulerService.createScheduler(savedGroup);

        return group.getId();
    }

    /**
     * 모든 그룹 페이징 검색
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public AllGroupDataResponse findGroups(Pageable pageable) {
        // Slice 객체로 Form 데이터를 가져옴
        Slice<GroupsPagingForm> allGroupForSlice = groupRepository.findAllGroupForSlice(pageable);
        // 아카이브 데이터를 리스트로 변환
        List<GroupsPagingForm> allGroupForList = allGroupForSlice.getContent();
        //페이징 처리를 위한 펌
        return AllGroupDataResponse.builder()
                .groupsPagingForms(allGroupForList)
                .offset(pageable.getOffset())
                .pageNum(allGroupForSlice.getNumber())
                .numberOfElements(allGroupForSlice.getNumberOfElements())
                .size(pageable.getPageSize())
                .isLast(allGroupForSlice.isLast())
                .build();
    }

    /**
     * 그룹 리더의 그룹 정보 검색
     * @param groupId
     */
    @Transactional(readOnly = true)
    public GroupForm findGroupByGroupId(Long groupId) {
        List<UserGroup> userGroup = userGroupRepository.joinUserGroupAndGroupFindAllUserGroupByGroupId(groupId);
        //stream을 통해 리더 찾기
        UserGroup Leader = findLeaderInUserGroup(userGroup);
        Group group = Leader.getGroup();
        return GroupForm.builder()
                .groupId(group.getId())
                .count(userGroup.size())
                .name(group.getName())
                .nickname(Leader.getNickname())
                .createDate(group.getCreateDate())
                .information(group.getInformation())
                .build();
    }

    /**
     * 패스워드로 그룹에 조인
     * @param groupId
     * @param form
     * @param requestUserId
     */
    @Transactional
    public GroupIdResponseForm joinGroup(Long groupId, JoinGroupForm form, Long requestUserId) {
        //group 조회
        Group group = groupRepository.findUserAndUserGroupAndGroupByid(groupId).orElseThrow(
                () -> new NotFoundContentsException("그룹 정보가 없습니다.")
        );
        List<UserGroup> userGroup = group.getUserGroup();
        //그룹에 참여한 유저인지 확인
        if (userGroupService.checkUserInGroup(requestUserId, userGroup)){
            throw new ExistUserException("이미 가입된 회원입니다.");
        }
        //데이터 요청자의 User 조회
        User requstUser = userService.findUserById(requestUserId);
        //공개 그룹인 경우
        if (group.getPassword() == null) {
            saveUserGroup(requstUser, group);
            return new GroupIdResponseForm(group.getId());
        }
        //패스워드가 입력된 경우 && 비공개 그룹인 경우
        if (StringUtils.hasText(form.getPassword()) &&
                passwordEncoder.matches(form.getPassword(), group.getPassword())) {
            saveUserGroup(requstUser, group);
            return new GroupIdResponseForm(group.getId());
        }
        throw new AccessDeniedException("패스워드가 다릅니다");
    }

    /**
     * 그룹 내 역할을 조회
     * @param groupId
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public List<UserGroupRoleSummary> getMemberRole(Long groupId, Long requestUserId) {
        //group 조회
        Group group = groupRepository.findUserAndUserGroupAndGroupByid(groupId).orElseThrow(
                () -> new NotFoundContentsException("그룹 정보가 없습니다.")
        );
        List<UserGroup> userGroup = group.getUserGroup();
        //그룹 내 회원인지 체크
        if (userGroupService.checkUserInGroup(requestUserId, userGroup)) {
            return userGroup.stream().map(
                    ug -> new UserGroupRoleSummary(ug.getId(), ug.getNickname(), ug.getRole())
            ).toList();
        }
        throw new AccessDeniedException("접근이 불가능한 정보입니다.");
    }

    /**
     * 그룹 내 유저의 역할을 업데이트
     * @param groupId
     * @param userGroupRoleSummaries
     * @param requestUserId
     * @return
     */
    @Transactional
    public GroupIdResponseForm updateMembersRole(Long groupId, List<UserGroupRoleSummary> userGroupRoleSummaries, Long requestUserId) {
        log.info("== [fetch Query] user, userGroup, group 조회");
        Group group = groupRepository.findUserAndUserGroupAndGroupByid(groupId)
                .orElseThrow(() -> new NotFoundContentsException("그룹 정보가 없습니다."));
        List<UserGroup> userGroups = group.getUserGroup();
        // 요청자 UserGroup 찾기 및 권한 체크
        UserGroup requestUserGroup = findRoleByRequestUserId(requestUserId, userGroups);
        Role requesterRole = requestUserGroup.getRole();
        if (!(requesterRole == Role.LEADER || requesterRole == Role.MANAGER)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        //
        Map<Long, UserGroup> userGroupMap = userGroups.stream()
                .collect(Collectors.toMap(UserGroup::getId, Function.identity()));
        // Role 별로 변경할 UserGroup ID 목록을 담을 Map
        Map<Role, List<Long>> roleToUserGroupIds = new EnumMap<>(Role.class);
        boolean newLeaderAssigned = false;

        for (UserGroupRoleSummary summary : userGroupRoleSummaries) {
            UserGroup target = userGroupMap.get(summary.getId());
            if (target == null) continue;

            Role currentRole = target.getRole();
            Role newRole = summary.getRole();
            // 변경 필요 없으면 skip
            if (currentRole == newRole) continue;
            // 아무도 리더의 권한을 변경할 수 없다.
            if (currentRole == Role.LEADER) {
                throw new NotAllowException("리더는 직접 변경 불가합니다.");
            }
            // 매니저는 리더 매니저의 권한을 변경할 수 없다.
            if (requesterRole == Role.MANAGER && currentRole == Role.MANAGER)
                throw new NotAllowException("매니저 권한 밖 입니다");
            // 리더는 리더의 권한을 위임을 통해서 한명만 선택해 변경된다.
            if (requesterRole == Role.LEADER && newRole == Role.LEADER) {
                if (newLeaderAssigned) {
                    throw new ExistUserException("이미 새 리더가 존재합니다");
                }
                userGroupRepository.updateUserGroupRole(requestUserGroup.getId(), Role.MEMBER);
                newLeaderAssigned = true;
            }
            // Role 별 UserGroup ID 리스트에 추가
            roleToUserGroupIds.computeIfAbsent(newRole, k -> new ArrayList<>()).add(summary.getId());
        }

        // Role 별 벌크 업데이트 실행
        for (Map.Entry<Role, List<Long>> entry : roleToUserGroupIds.entrySet()) {
            Role role = entry.getKey();
            List<Long> ids = entry.getValue();
            userGroupRepository.bulkUpdateUserGroupRoles(ids, role);
        }

        log.info("== [벌크 업데이트 완료]");
        return new GroupIdResponseForm(groupId);
    }

    /**
     * 그룹 내 요청자 role 찾기
     * @param requestUserId
     * @param userGroup
     * @return
     */
    private UserGroup findRoleByRequestUserId(Long requestUserId, List<UserGroup> userGroup) {
        return userGroup.stream()
                .filter(ug -> ug.getUser().getId().equals(requestUserId))
                .findFirst()
                .orElseThrow(() -> new NotFoundUserException("유저의 정보를 찾을 수 없습니다."));
    }

    /**
     * 그룹내 리더 찾기
     * @param userGroup
     * @return
     */
    private static UserGroup findLeaderInUserGroup(List<UserGroup> userGroup) {
        return userGroup.stream().filter(ug -> ug.getRole().equals(Role.LEADER))
                .findFirst()
                .orElseThrow(() -> new NotFoundContentsException("리더가 존재하지 않습니다."));
    }

    /**
     * 그룹에 입장
     * @param user
     * @param group
     */
    private void saveUserGroup(User user, Group group) {
        UserGroup userGroup = new UserGroup(user.getName(), user, group, Role.GUEST);
        userGroupRepository.save(userGroup);
    }

    /**
     * 그룹 비밀번호 체크 및 그룹 생성
     * @param requestForm
     * @return
     */
    private Group buildGroupFromRequest(GroupRequestForm requestForm) {
        String password = requestForm.getPassword();
        String checkPassword = requestForm.getCheckPassword();
        //password 가 blank 인지 체크 // 공개 그룹 생성
        if (!StringUtils.hasText(password) && !StringUtils.hasText(checkPassword)) {
            return new Group(requestForm.getGroupType(), requestForm.getGroupName(), requestForm.getInfo());
        }
        //password != checkPassword
        if (!StringUtils.hasText(password) || !password.equals(checkPassword)) {
            throw new NotValidException("패스워드를 확인해주세요");
        }
        //비공개 그룹 생성
        return new Group(
                passwordEncoder.encode(password),
                requestForm.getGroupType(),
                requestForm.getGroupName(),
                requestForm.getInfo()
        );
    }
}
