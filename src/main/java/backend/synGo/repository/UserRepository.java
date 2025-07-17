package backend.synGo.repository;


import backend.synGo.domain.user.Provider;
import backend.synGo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Boolean existsUserByEmail(String email);
    // OAuth2를 위한 새 메소드들
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
    Optional<User> findByEmailAndProvider(String email, Provider provider);
}
