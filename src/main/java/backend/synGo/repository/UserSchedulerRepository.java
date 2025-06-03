package backend.synGo.repository;

import backend.synGo.domain.schedule.UserScheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSchedulerRepository extends JpaRepository<UserScheduler, Long> {
    @Query("select us from UserScheduler us join fetch us.user u where u.id=:userId")
    UserScheduler findSchedulerAndUserByUserId(@Param("userId") Long userId);
}
