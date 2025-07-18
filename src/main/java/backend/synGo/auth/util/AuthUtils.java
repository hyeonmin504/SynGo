package backend.synGo.auth.util;

import backend.synGo.auth.form.AuthenticatedUser;
import backend.synGo.auth.oauth2.CustomOAuth2User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class AuthUtils {

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // OAuth2 사용자와 일반 JWT 사용자 모두 지원
        if (principal instanceof AuthenticatedUser) {
            return (AuthenticatedUser) principal;
        }

        // AnonymousAuthenticationToken 등 처리
        if ("anonymousUser".equals(principal)) {
            return null;
        }

        log.warn("예상하지 못한 principal 타입: {}", principal.getClass());
        return null;
    }

    public static Long getCurrentUserId() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getCurrentUserEmail() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    // 현재 사용자가 OAuth2 사용자인지 확인
    public static boolean isOAuth2User() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof CustomOAuth2User;
    }
}