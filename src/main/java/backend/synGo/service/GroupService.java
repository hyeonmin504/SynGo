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
    private final PasswordEncoder encoder;

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
        UserGroup userGroup = userGroupRepository.findById(userGroupId).orElseThrow(
                () -> new NotFoundContentsException("해당 유저의 그룹을 찾을 수 없습니다")
        );
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
        //userGroup 조회
        UserGroup userGroup = userGroupRepository.findById(userGroupId)
                .orElseThrow(() -> new NotFoundContentsException("해당 그룹을 찾을 수 없습니다."));
        Group group = userGroup.getGroup();
        //요청자가 그룹에 이미 가입되어 있는지 확인
        if (userGroupRepository.existsByGroupAndUserId(group, requestUserId)) {
            throw new AccessDeniedException("이미 가입된 회원입니다.");
        }
        //데이터 요청자의 User 조회
        User requstUser = userService.findUserById(requestUserId);
        //공개 그룹인 경우
        if (group.getPassword() == null) {
            saveUserGroup(requstUser, group);
            return ;
        }
        log.info(String.valueOf(StringUtils.hasText(form.getPassword())));
        //패스워드가 입력된 경우 && 비공개 그룹인 경우
        if (StringUtils.hasText(form.getPassword()) &&
                passwordEncoder.matches(form.getPassword(), group.getPassword())) {
            saveUserGroup(requstUser, group);
            return ;
        }
        throw new AccessDeniedException("패스워드가 다릅니다");
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
