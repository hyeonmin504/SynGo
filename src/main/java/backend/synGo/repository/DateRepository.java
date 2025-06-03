package backend.synGo.repository;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.user.User;
import backend.synGo.repository.query.DateRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// DateRepository.java
public interface DateRepository extends JpaRepository<Date, Long>, DateRepositoryQuery {
    Optional<Date> findByStartDateAndUser(LocalDate startDate, User user);

    @Query("select d from Date d join fetch d.userSlot where d.user.id=:userId AND d.startDate=:startDate")
    Optional<Date> findDateAndUserSlotByStartDateAndUserId(@Param("startDate") LocalDate startDate,@Param("userId") Long userId);
}
