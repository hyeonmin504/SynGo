package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.form.requestForm.JoinGroupForm;
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

import java.util.List;
import java.util.Map;
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
        UserGroup userGroup = userGroupService.saveUserGroupData(requestForm.getNickname(), userService.findUserById(userId), savedGroup, Role.LEADER);
        //groupScheduler 생성및 저장
        groupSchedulerService.createScheduler(savedGroup);

        return userGroup.getId();
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
     * @param userGroupId
     */
    @Transactional(readOnly = true)
    public GroupForm findGroupByGroupId(Long userGroupId) {
        UserGroup userGroup = userGroupService.findGroupByUserGropId(userGroupId);
        Group group = userGroup.getGroup();

        List<UserGroup> byGroup = userGroupRepository.findByGroup(group);
        return GroupForm.builder()
                .userGroupId(userGroupId)
                .count(byGroup.size())
                .name(group.getName())
                .nickname(userGroup.getNickname())
                .createDate(group.getCreateDate())
                .information(group.getInformation())
                .build();
    }

    /**
     * 패스워드로 그룹에 조인
     * @param userGroupId
     * @param form
     * @param requestUserId
     */
    @Transactional
    public void joinGroup(Long userGroupId, JoinGroupForm form, Long requestUserId) {
        //group 조회
        Group group = userGroupService.findGroupByUserGropId(userGroupId).getGroup();
        //그룹에 참여한 유저인지 확인
        if (checkUserInGroup(requestUserId, group)){
            throw new AccessDeniedException("이미 가입된 회원입니다.");
        }
        //데이터 요청자의 User 조회
        User requstUser = userService.findUserById(requestUserId);
        //공개 그룹인 경우
        if (group.getPassword() == null) {
            saveUserGroup(requstUser, group);
            return ;
        }
        //패스워드가 입력된 경우 && 비공개 그룹인 경우
        if (StringUtils.hasText(form.getPassword()) &&
                passwordEncoder.matches(form.getPassword(), group.getPassword())) {
            saveUserGroup(requstUser, group);
            return ;
        }
        throw new AccessDeniedException("패스워드가 다릅니다");
    }

    /**
     * 그룹 내 역할을 조회
     * @param userGroupId
     * @param requestUserId
     * @return
     */
    @Transactional(readOnly = true)
    public List<UserGroupRoleSummary> getMemberRole(Long userGroupId, Long requestUserId) {
        //group 조회
        Group group = userGroupService.findGroupByUserGropId(userGroupId).getGroup();
        //그룹 내 회원인지 체크
        if (checkUserInGroup(requestUserId, group)) {
            return userGroupRepository.findByGroup(group).stream().map(
                    userGroup -> new UserGroupRoleSummary(userGroup.getId(), userGroup.getNickname(), userGroup.getRole())
            ).toList();
        }
        throw new AccessDeniedException("접근이 불가능한 정보입니다.");
    }

    /**
     * 그룹 내 유저의 역할을 업데이트
     * @param userGroupId
     * @param userGroupRoleSummaries
     * @param requestUserId
     * @return
     */
    @Transactional
    public List<UserGroupRoleSummary> updateMembersRole(Long userGroupId, List<UserGroupRoleSummary> userGroupRoleSummaries, Long requestUserId) {
        //group 조회
        log.info("== [1st Query] userGroupId로 그룹 조회");
        Group group = userGroupService.findGroupByUserGropId(userGroupId).getGroup();
        //요청자가 그룹에 속하는지 확인
        log.info("== [2nd Query] 그룹 내 요청자 유효성 검사");
        UserGroup requestUserGroup = userGroupRepository.findByGroupAndUserId(group, requestUserId)
                .orElseThrow(() -> new AccessDeniedException("그룹에 속한 사용자가 아닙니다."));
        //요청자의 Role이 LEADER 혹은 MANAGER인지 체크
        Role requesterRole = requestUserGroup.getRole();
        if (!(requesterRole == Role.LEADER || requesterRole == Role.MANAGER)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
        log.info("== [3rd Query] LAZY 로딩으로 group 정보 접근");
        List<UserGroup> userGroups = group.getUserGroup();
        log.info("== [4th Query] batch fetch로 전체 UserGroup 조회 및 매핑");
        Map<Long, UserGroup> userGroupMap = userGroups.stream()
                .collect(Collectors.toMap(UserGroup::getId, ug -> ug)); //id → UserGroup 매핑
        log.info("4th query after");
        // 역할을 업데이트, batch size를 통해 n+1 문제를 해결
        for (UserGroupRoleSummary summary : userGroupRoleSummaries) {
            UserGroup targetUserGroup = userGroupMap.get(summary.getId());

            if (targetUserGroup == null) continue; // 없는 ID 건너뛰기

            Role currentRole = targetUserGroup.getRole();
            Role newRole = summary.getRole();

            // 리더는 건들 수 없음
            if (currentRole == Role.LEADER) continue;

            // 매니저를 매니저가 바꾸려 하면 불가
            if (currentRole == Role.MANAGER && requesterRole != Role.LEADER) continue;

            // 요청자가 LEADER이고 다른 유저를 LEADER로 바꾸려면 본인 권한은 MEMBER로 강등
            if (requesterRole == Role.LEADER && newRole == Role.LEADER) {
                requestUserGroup.setRole(Role.MEMBER);
            }

            targetUserGroup.setRole(newRole);
        }
        log.info("== [5th/6th Query] 역할 수정 후 전체 조회 및 DTO 반환");
        // 수정된 결과 반환
        return userGroupRepository.findByGroup(group).stream()
                .map(ug -> new UserGroupRoleSummary(ug.getId(), ug.getNickname(), ug.getRole()))
                .toList();
    }

    private boolean checkUserInGroup(Long requestUserId, Group group) {
        //요청자가 그룹에 이미 가입되어 있는지 확인
        return userGroupRepository.existsByGroupAndUserId(group, requestUserId);
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
