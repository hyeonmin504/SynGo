package backend.synGo.repository;

import backend.synGo.domain.group.Group;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.domain.userGroupData.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    List<UserGroup> findByGroup(Group group);

    // UserGroupRepository.java
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    @Query("select ug from UserGroup ug where ug.user.id=:userId And ug.group.id=:groupId")
    Optional<UserGroup> findByGroupIdAndUserId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("select ug from UserGroup ug join fetch ug.group g where g.id=:groupId")
    List<UserGroup> joinUserGroupAndGroupFindAllUserGroupByGroupId(@Param("groupId") Long groupId);

    @Modifying(clearAutomatically = true)
    @Query("update UserGroup ug set ug.role = :role where ug.id =:targetId")
    void updateUserGroupRole(@Param("targetId") Long targetId, @Param("role") Role role);

    @Modifying(clearAutomatically = true)
    @Query("update UserGroup ug set ug.role = :role where ug.id in :targetIds")
    void bulkUpdateUserGroupRoles(@Param("targetIds") List<Long> targetIds, @Param("role") Role role);
}
