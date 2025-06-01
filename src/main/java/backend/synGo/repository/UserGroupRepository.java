package backend.synGo.repository;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.userGroupData.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByGroup(Group group);

    // UserGroupRepository.java
    boolean existsByGroupAndUserId(Group group, Long userId);
}
