package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import backend.synGo.repository.GroupRepository;
import backend.synGo.repository.UserGroupRepository;
import backend.synGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GroupServiceTest {

    @Autowired GroupService groupService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("테스터");
        userRepository.save(user);
    }
    @Test
    void 공개_그룹_생성_성공() {
        // given
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName("스터디 그룹")
                .nickname("홍길동")
                .info("설명")
                .groupType(GroupType.BASIC)
                .build();

        User user = new User("테스터");
        userRepository.save(user); // 실제 저장

        // when
        Long userGroupId = groupService.createGroupAndReturnUserGroupId(form, user.getId());

        // then
        assertThat(userGroupId).isNotNull();
    }

    @Test
    void 비공개_그룹_생성_성공() {
        // given
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName("비공개 스터디")
                .nickname("닉네임")
                .info("소개")
                .password("1234")
                .checkPassword("1234")
                .groupType(GroupType.BASIC)
                .build();

        User user = new User("테스터");
        userRepository.save(user);

        // when
        Long userGroupId = groupService.createGroupAndReturnUserGroupId(form, user.getId());

        // then
        assertThat(userGroupId).isNotNull();
    }

    @Test
    void 비밀번호_불일치_예외() {
        // given
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName("실패 케이스")
                .nickname("홍길동")
                .info("소개")
                .password("1234")
                .checkPassword("abcd") // 불일치
                .groupType(GroupType.BASIC)
                .build();

        // then
        assertThatThrownBy(() ->
                groupService.createGroupAndReturnUserGroupId(form, 1L))
                .isInstanceOf(NotValidException.class);
    }

    @Test
    void 공개_그룹_참여_성공() {
        Group group = new Group(GroupType.BASIC, "공개 그룹", "설명");
        groupRepository.save(group);

        UserGroup leader = new UserGroup("방장", user, group, Role.LEADER);
        userGroupRepository.save(leader);

        User otherUser = new User("신규유저");
        userRepository.save(otherUser);

        groupService.joinGroup(leader.getId(), new JoinGroupForm(null), otherUser.getId());

        assertThat(userGroupRepository.existsByGroupAndUserId(group, otherUser.getId())).isTrue();
    }

    @Test
    void 비공개_그룹_참여_성공() {
        String rawPassword = "group1234";
        Group group = new Group(passwordEncoder.encode(rawPassword), GroupType.BASIC, "비공개", "소개");
        groupRepository.save(group);

        UserGroup leader = new UserGroup("방장", user, group, Role.LEADER);
        userGroupRepository.save(leader);

        User otherUser = new User("참여자");
        userRepository.save(otherUser);

        groupService.joinGroup(leader.getId(), new JoinGroupForm(rawPassword), otherUser.getId());

        assertThat(userGroupRepository.existsByGroupAndUserId(group, otherUser.getId())).isTrue();
    }

    @Test
    void 비공개_그룹_비밀번호_불일치_예외() {
        Group group = new Group(passwordEncoder.encode("1234"), GroupType.BASIC, "비공개", "소개");
        groupRepository.save(group);

        UserGroup leader = new UserGroup("방장", user, group, Role.LEADER);
        userGroupRepository.save(leader);

        User otherUser = new User("참여자");
        userRepository.save(otherUser);

        assertThatThrownBy(() -> groupService.joinGroup(leader.getId(), new JoinGroupForm("wrong"), otherUser.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("패스워드가 다릅니다");
    }

    @Test
    void 이미_가입된_사용자_예외() {
        Group group = new Group(GroupType.BASIC, "공개", "소개");
        groupRepository.save(group);

        UserGroup leader = new UserGroup("방장", user, group, Role.LEADER);
        userGroupRepository.save(leader);

        assertThatThrownBy(() -> groupService.joinGroup(leader.getId(), new JoinGroupForm(null), user.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("이미 가입된 회원입니다");
    }

    @Test
    void 존재하지_않는_그룹_예외() {
        assertThatThrownBy(() -> groupService.joinGroup(9999L, new JoinGroupForm("1234"), user.getId()))
                .isInstanceOf(NotFoundContentsException.class)
                .hasMessageContaining("해당 그룹을 찾을 수 없습니다");
    }
}
