package backend.synGo.auth.service;


import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.form.LoginResponseForm;
import backend.synGo.auth.form.TokenType;
import backend.synGo.config.jwt.JwtProvider;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.user.User;
import backend.synGo.exception.AuthenticationFailedException;
import backend.synGo.exception.ExistUserException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.repository.UserRepository;
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

    @Transactional(readOnly = true)
    public LoginResponseForm login(LoginForm form, HttpServletRequest request) {

        User user = userRepository.findByEmail(form.getEmail()).orElseThrow(
                () -> new NotFoundUserException("이메일이 잘못되었습니다.")
        );

        if (!user.getLastAccessIp().equals(ClientIp.getClientIp(request))){
            //todo : ip가 다를 경우 로직

            //ip 저장
            user.setNewUserIp(ClientIp.getClientIp(request));
            userRepository.save(user);
        }

        if (passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getName(), user.getLastAccessIp());
            String accessToken = jwtProvider.createAccessToken(userDetails);
            String refreshToken = jwtProvider.createRefreshToken(userDetails);

            return new LoginResponseForm(accessToken, refreshToken);
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
                UserScheduler userScheduler = new UserScheduler(Theme.BLACK);
                userRepository.save(new User(form.getName(),form.getEmail(),passwordEncoder.encode(form.getPassword()),ClientIp.getClientIp(request),userScheduler));
                return ;
            }
            throw new NotValidException("패스워드가 유효하지 않습니다.");
        }
        throw new NotValidException("패스워드가 동일하지 않습니다");
    }

    public void logout(HttpServletRequest request) {
        String token = jwtProvider.resolveToken(request);

        if (token != null && jwtProvider.validateToken(token, TokenType.AT)) {
            // (Optional) AccessToken을 블랙리스트에 등록 (만료 시간까지)
            jwtProvider.blackListToken(token);

            // AccessToken에서 userId 추출
            Authentication authentication = jwtProvider.getAuthentication(token);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserId();

            // Redis에서 RefreshToken 삭제
            jwtProvider.deleteRefreshToken(userId);
            // Redis에서 AccessToken 삭제
            jwtProvider.deleteAccessToken(userId);
            return ;
        }
        throw new AuthenticationFailedException("유효하지 않은 토큰입니다.");
    }

    public String reissue(HttpServletRequest request) {
        // 1. Refresh Token 유효성 검사, 유저 정보 추출
        CustomUserDetails userDetails = readTokenAndReturnUserId(request);
        Long userId = userDetails.getUserId();

        //기본 accessToken을 블랙리스트에 올리기
        jwtProvider.deleteAccessToken(userId);

        //유저 조회
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundUserException("유저 정보가 없습니다"));

        //ip 체크
        if (!userDetails.getUserLastAccessIp().equals(user.getLastAccessIp())){
            //todo: ip 접속 환경이 다를 경우 어떻게 처리 할 것인지.
        }

        // 4. 새로운 Access Token 생성
        String newAccessToken = jwtProvider.createAccessToken(userDetails);

        // (선택) 새 Refresh Token도 함께 발급
        // String newRefreshToken = jwtProvider.createRefreshToken(userId, ...);
        // redisTemplate.opsForValue().set("RT:" + userId, newRefreshToken, ...);

        return newAccessToken;
    }

    public CustomUserDetails readTokenAndReturnUserId(HttpServletRequest request) {
        String refreshToken = jwtProvider.resolveToken(request);

        // 2. Claims 추출 (유저 정보)
        Authentication authentication = jwtProvider.getAuthentication(refreshToken);
        return (CustomUserDetails)authentication.getPrincipal();
    }
}