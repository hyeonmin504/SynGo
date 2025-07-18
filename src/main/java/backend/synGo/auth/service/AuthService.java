package backend.synGo.auth.service;


import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.form.TokenResponseForm;
import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.user.User;
import backend.synGo.exception.AuthenticationFailedException;
import backend.synGo.exception.ExistUserException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.repository.UserRepository;
import backend.synGo.service.ThemeService;
import backend.synGo.service.UserService;
import backend.synGo.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static backend.synGo.auth.util.PasswordValidator.isValid;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ThemeService themeService;

    @Transactional
    public TokenResponseForm login(LoginForm form, HttpServletRequest request) {

        User user = userRepository.findByEmail(form.getEmail()).orElseThrow(
                () -> new NotFoundUserException("이메일이 잘못되었습니다.")
        );

        if (!user.getLastAccessIp().equals(ClientIp.getClientIp(request))){
            //todo : ip가 다를 경우 로직

            //ip 저장
            user.setNewUserIp(ClientIp.getClientIp(request));
        }

        if (passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String accessToken = jwtProvider.createAccessToken(userDetails);
            String refreshToken = jwtProvider.createRefreshToken(userDetails);

            return new TokenResponseForm(accessToken, refreshToken);
        }

        throw new NotValidException("패스워드가 잘못되었습니다");
    }

    @Transactional
    public void signUp(SignUpForm form, HttpServletRequest request) {
        if(form.getPassword().equals(form.getCheckPassword())) { // 패스워드 동일한지 체크
            if (isValid(form.getPassword())) { // 패스워드가 유효한지 체크
                if (userRepository.existsUserByEmail(form.getEmail())) { // 가입된 이메일이 존재하는지 체크
                    throw new ExistUserException("이미 존재하는 이메일입니다");
                }
                log.info("saveUser");
                UserScheduler userScheduler = new UserScheduler(themeService.getTheme(Theme.BLACK));
                userRepository.save(User.builder()
                        .name(form.getName())
                        .email(form.getEmail())
                        .password(passwordEncoder.encode(form.getPassword()))
                        .lastAccessIp(ClientIp.getClientIp(request))
                        .scheduler(userScheduler)
                        .build());
                return ;
            }
            throw new NotValidException("패스워드가 유효하지 않습니다.");
        }
        throw new NotValidException("패스워드가 동일하지 않습니다");
    }

    public void logout(HttpServletRequest request) {
        String token = jwtProvider.resolveToken(request);

        if (token != null && jwtProvider.validateToken(token)) {
            // (Optional) AccessToken을 블랙리스트에 등록 (만료 시간까지)
            jwtProvider.blackListToken(token);

            // AccessToken에서 userId 추출
            Authentication authentication = jwtProvider.getAuthentication(token);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();

            // Redis에서 RefreshToken 삭제
            jwtProvider.deleteRefreshToken(userId);
            return ;
        }
        throw new AuthenticationFailedException("유효하지 않은 토큰입니다.");
    }

    public TokenResponseForm reissue(HttpServletRequest request) {
        // 1. Refresh Token 유효성 검사, 유저 정보 추출
        CustomUserDetails userDetails = readTokenAndReturnUserId(request);
        Long userId = userDetails.getUserId();
        //유저 조회
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundUserException("유저 정보가 없습니다"));

        String token = jwtProvider.resolveToken(request);

        if (jwtProvider.validateRefreshToken(token)){
            jwtProvider.blackListToken(token);
            jwtProvider.deleteRefreshToken(userId);
            // 새로운 Access Token 생성
            String newAccessToken = jwtProvider.createAccessToken(userDetails);
            String newRefreshToken = jwtProvider.createRefreshToken(userDetails);
            return new TokenResponseForm(newAccessToken, newRefreshToken);
        }
        throw new NotValidException("인증 실패");
    }

    public CustomUserDetails readTokenAndReturnUserId(HttpServletRequest request) {
        String accessToken = jwtProvider.resolveToken(request);

        // 2. Claims 추출 (유저 정보)
        Authentication authentication = jwtProvider.getAuthentication(accessToken);
        return (CustomUserDetails)authentication.getPrincipal();
    }
}