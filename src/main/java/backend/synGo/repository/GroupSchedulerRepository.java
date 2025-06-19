package backend.synGo.repository;

import backend.synGo.domain.schedule.GroupScheduler;
import backend.synGo.domain.schedule.UserScheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupSchedulerRepository extends JpaRepository<GroupScheduler, Long> {
    @Query("select gs from GroupScheduler gs join fetch gs.theme t join fetch gs.group g where g.id=:groupId")
    GroupScheduler findSchedulerAndGroupByGroupId(@Param("groupId") Long groupId);
}
