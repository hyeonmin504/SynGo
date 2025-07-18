package backend.synGo.service;

import backend.synGo.auth.service.AuthService;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundUserException("유저 정보 없음")
        );
    }

    public User findUserByToken(HttpServletRequest request){
        Long userId = authService.readTokenAndReturnUserId(request).getUserId();

        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundUserException("유저 정보 없음")
        );
    }
}
