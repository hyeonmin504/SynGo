package backend.synGo.repository;

import backend.synGo.auth.oauth2.domain.UserOAuthConnection;
import backend.synGo.domain.user.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserOAuthConnectionRepository extends JpaRepository<UserOAuthConnection, Long> {
    Optional<UserOAuthConnection> findByProvider(Provider provider);

    @Query("select uc from UserOAuthConnection uc join uc.user u where u.id =:userId and uc.provider =:provider")
    Optional<UserOAuthConnection> findByUserIdAndProvider(Long userId, Provider provider);

    @Query("select uc from UserOAuthConnection uc join uc.user u where u.id =:userId")
    Optional<UserOAuthConnection> findByConnectionUserId(Long userId);
    Optional<UserOAuthConnection> findByProviderAndEmail(Provider provider, String email);
}
