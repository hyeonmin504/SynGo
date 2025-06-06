package backend.synGo.repository;

import backend.synGo.domain.group.Group;
import backend.synGo.repository.query.GroupRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> , GroupRepositoryQuery {
    @Query("select distinct g from GroupBasic g join fetch g.userGroup ug join fetch ug.user u where g.id=:groupId")
    Optional<Group> joinUserGroupAndUserByGroupId(@Param("groupId") Long groupId);


}
