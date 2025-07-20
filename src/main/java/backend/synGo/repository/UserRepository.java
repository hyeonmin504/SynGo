package backend.synGo.repository;


import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    @Query("select u from User u join fetch u.userOAuthConnection uc where u.email =:email")
    Optional<User> findByEmailWithUserOAuthConnection(String email);
    Boolean existsUserByEmail(String email);
}
