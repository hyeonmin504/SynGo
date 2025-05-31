package backend.synGo.repository;

import backend.synGo.domain.group.Group;
import backend.synGo.repository.query.GroupRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> , GroupRepositoryQuery {
}
