package backend.synGo.auth;

import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public CustomUserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("user 검색중");
        User user = userRepository.findById(Long.parseLong(userId)).orElseThrow(
                () -> new NotFoundUserException("사용자를 찾을 수 없습니다")
        );
        log.info("user 검색끝");

        return new CustomUserDetails(user.getId(),user.getName(),user.getLastAccessIp());
    }
}
