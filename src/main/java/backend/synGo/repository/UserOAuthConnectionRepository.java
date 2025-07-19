package backend.synGo.repository;

import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.domain.user.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOAuthConnectionRepository extends JpaRepository<UserOAuthConnection, Long> {
    Optional<UserOAuthConnection> findByProvider(Provider provider);
    Optional<UserOAuthConnection> findByUserIdAndProvider(Long userId, Provider provider);
    List<UserOAuthConnection> findByUserId(Long userId);

    Optional<UserOAuthConnection> findByProviderAndEmail(Provider provider, String email);
}
