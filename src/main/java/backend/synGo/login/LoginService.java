package backend.synGo.login;


import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.domain.user.User;
import backend.synGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResponseDto login(String name, String password) {

        User user1 = userRepository.findById(1L).get();

        if (!passwordEncoder.matches(password, user1.getPassword())) {
            return new LoginResponseDto("로그인 실패", null,null);
        }

        String accessToken = jwtProvider.createAccessToken(user1);
        String refreshToken = jwtProvider.createRefreshToken(user1);

        return new LoginResponseDto("로그인 성공", accessToken, refreshToken);
    }

    @Transactional
    public void signin(String password) {
        User user = new User("name", "ip1231242");
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
