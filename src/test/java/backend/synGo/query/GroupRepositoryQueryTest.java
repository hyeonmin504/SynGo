package backend.synGo.query;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.user.User;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.repository.GroupRepository;
import backend.synGo.repository.UserGroupRepository;
import backend.synGo.repository.UserRepository;
import backend.synGo.repository.query.impl.GroupRepositoryQueryImpl;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class GroupRepositoryQueryTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    UserGroupRepository userGroupRepository;
    @Autowired
    GroupRepositoryQueryImpl groupRepositoryQueryImpl;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
        groupRepository.deleteAll();
        userGroupRepository.deleteAll();
    }

    @Test
    @DisplayName("그룹 전체 조회 페이징 쿼리 테스트")
    public void 그룹_조회_쿼리_정상작동_확인() {
        // given: 데이터 삽입
        User user = new User("테스터");
        userRepository.save(user);

        Group group = new Group(GroupType.BASIC, "테스트 그룹", "설명");
        groupRepository.save(group);

        UserGroup userGroup = new UserGroup("병장", user, group, Role.LEADER);
        userGroupRepository.save(userGroup);

        // when
        PageRequest pageable = PageRequest.of(0, 10);
        Slice<GroupsPagingForm> result = groupRepositoryQueryImpl.findAllGroupForSlice(pageable);

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).anyMatch(g -> g.getName().equals("테스트 그룹"));
    }

}