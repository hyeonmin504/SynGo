package backend.synGo.repository;

import backend.synGo.domain.schedule.UserScheduler;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSchedulerRepository extends JpaRepository<UserScheduler, Long> {
}
