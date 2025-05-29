package backend.synGo.repository;

import backend.synGo.domain.date.Date;
import backend.synGo.domain.user.User;
import backend.synGo.repository.query.DateRepositoryQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// DateRepository.java
public interface DateRepository extends JpaRepository<Date, Long>, DateRepositoryQuery {
    Optional<Date> findByStartDateAndUser(LocalDate startDate, User user);

}
