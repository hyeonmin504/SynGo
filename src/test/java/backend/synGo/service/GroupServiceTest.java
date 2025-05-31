package backend.synGo.service;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup; // ✅ 추가: UserGroup import
import backend.synGo.exception.NotValidException;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.repository.GroupRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @InjectMocks
    private GroupService groupService;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupService userGroupService;

    @Mock
    private UserService userService;

    @Mock
    private GroupSchedulerService groupSchedulerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void 공개_그룹_생성_성공() {
        GroupRequestForm publicForm = GroupRequestForm.builder()
                .groupName("스터디 그룹")
                .nickname("홍길동")
                .info("자바 스터디")
                .groupType(GroupType.BASIC)
                .build();

        User mockUser = new User(1L);

        when(userService.findUserById(anyLong())).thenReturn(mockUser);

        // ✅ groupRepository.save() Mock 설정
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            group.setId(1L);
            return group;
        });

        // ✅ 변경된 반환값 처리: saveUserGroupData()가 UserGroup 반환
        when(userGroupService.saveUserGroupData(any(), any(), any(), eq(Role.LEADER)))
                .thenAnswer(invocation -> {
                    UserGroup ug = new UserGroup(100L);
                    return ug;
                });

        // ✅ 반환값 변수명 수정: groupId → userGroupId
        Long userGroupId = groupService.createGroupAndReturnUserGroupId(publicForm, 1L);

        // ✅ 예상 반환값 검증
        Assertions.assertEquals(100L, userGroupId, "리턴된 userGroupId는 100이어야 한다.");

        verify(groupRepository).save(any(Group.class));
        verify(userGroupService).saveUserGroupData(any(), any(), any(), eq(Role.LEADER));
        verify(groupSchedulerService).createScheduler(any());
    }

    @Test
    void 비공개_그룹_생성_성공() {
        GroupRequestForm privateForm = GroupRequestForm.builder()
                .groupName("비밀 스터디")
                .nickname("홍길동")
                .info("비공개 스터디입니다")
                .password("1234")
                .checkPassword("1234")
                .groupType(GroupType.BASIC)
                .build();

        User mockUser = new User(1L);

        when(userService.findUserById(anyLong())).thenReturn(mockUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            group.setId(2L);
            return group;
        });

        // ✅ 반환값 UserGroup ID로 설정
        when(userGroupService.saveUserGroupData(any(), any(), any(), eq(Role.LEADER)))
                .thenAnswer(invocation -> {
                    UserGroup ug = new UserGroup(200L);
                    return ug;
                });

        Long userGroupId = groupService.createGroupAndReturnUserGroupId(privateForm, 1L);

        // ✅ 예상 반환값 검증
        Assertions.assertEquals(200L, userGroupId, "리턴된 userGroupId는 200이어야 한다.");

        verify(groupRepository).save(any(Group.class));
        verify(userGroupService).saveUserGroupData(any(), any(), any(), eq(Role.LEADER));
        verify(groupSchedulerService).createScheduler(any());
    }

    @Test
    void 비밀번호_불일치_시_예외발생() {
        GroupRequestForm invalidForm = GroupRequestForm.builder()
                .groupName("비밀번호 불일치")
                .nickname("홍길동")
                .info("실패 케이스")
                .password("1234")
                .checkPassword("abcd")
                .groupType(GroupType.BASIC)
                .build();

        Assertions.assertThrows(NotValidException.class, () -> {
            groupService.createGroupAndReturnUserGroupId(invalidForm, 1L);
        });
    }
}