package backend.synGo.repository;

import backend.synGo.domain.schedule.GroupScheduler;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupSchedulerRepository extends JpaRepository<GroupScheduler, Long> {
}
