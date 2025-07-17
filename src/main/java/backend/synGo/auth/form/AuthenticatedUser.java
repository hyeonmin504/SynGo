package backend.synGo.auth.form;

import backend.synGo.domain.user.Provider;

public interface AuthenticatedUser {
    Long getUserId();
    String getName();
    String getEmail();
    String getLastAccessIp();
    Provider getProvider();
    String getProfileImageUrl();
}
