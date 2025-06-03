package backend.synGo.repository;


import backend.synGo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsUserByEmail(String email);

    @Query("select u from User u join fetch u.date d where u.id=:userId AND d.startDate=:startDate")
    Optional<User> findUserDateData(Long userId, LocalDate startDate);
}
