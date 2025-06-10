package backend.synGo.auth.controller;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.user.User;
import backend.synGo.exception.ExpiredTokenException;
import backend.synGo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {
    @Value("${security.secret-key}")
    String secretKey;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        UserScheduler scheduler = new UserScheduler(Theme.BLACK);
        User user = new User(
                "name"
                ,"test@naver.com"
                ,passwordEncoder.encode("Qwer1234!")
                ,"127.0.0.1"
                ,scheduler);
        userRepository.save(user);
    }

    @Test
    @DisplayName("정상 회원가입 api 테스트")
    public void signUpTest() throws Exception{
        //given
        SignUpForm request = SignUpForm.builder()
                .email("321@naver.com")
                .name("kim")
                .password("Hoon0504!")
                .checkPassword("Hoon0504!")
                .build();
        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));

        //then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("정상 로그인 api 테스트")
    public void loginTest() throws Exception {
        //given
        LoginForm form = LoginForm.builder()
                .email("test@naver.com")
                .password("Qwer1234!")
                .build();
        String json = objectMapper.writeValueAsString(form);

        //when
        ResultActions result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
        //then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.accessToken").exists()) // accessToken 존재 여부
                .andExpect(jsonPath("$.data.refreshToken").exists()) // refreshToken 존재 여부
                .andExpect(jsonPath("$.message").value("로그인 성공"));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 예외 테스트")
    public void signUpDuplicateEmailTest() throws Exception {
        SignUpForm request = SignUpForm.builder()
                .email("test@naver.com") // 이미 존재하는 이메일
                .name("someone")
                .password("Hoon0504!")
                .checkPassword("Hoon0504!")
                .build();
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk()) // 혹은 400/409 등 예외 코드
                .andExpect(jsonPath("$.code").value(HttpStatus.NOT_ACCEPTABLE.value()))
                .andExpect(jsonPath("$.message").value("이미 존재하는 이메일입니다")); // 예시
    }

    @Test
    @DisplayName("로그아웃 API - JWT 인증 필요")
    void logoutTest() throws Exception {
        // given
        // JWT 토큰 발급
        LoginForm loginForm = LoginForm.builder()
                .email("test@naver.com")
                .password("Qwer1234!")
                .build();
        String loginJson = objectMapper.writeValueAsString(loginForm);

        // 로그인 요청 → accessToken 얻기
        String accessToken = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // accessToken 파싱 (Jackson으로)
        String token = JsonPath.read(accessToken, "$.data.accessToken");

        // when: 로그아웃 요청 (JWT 포함)
        ResultActions result = mockMvc.perform(get("/api/auth/logout")
                .header("Authorization", "Bearer " + token));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));
    }

    @Test
    @DisplayName("Access Token 재발급 성공 테스트 (실제 로그인 기반)")
    public void reissue_success() throws Exception {
        // given - 로그인 요청
        LoginForm loginForm = LoginForm.builder()
                .email("test@naver.com")
                .password("Qwer1234!")
                .build();
        String loginJson = objectMapper.writeValueAsString(loginForm);

        // 로그인 → refreshToken 추출
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = JsonPath.read(loginResponse, "$.data.refreshToken");

        // when - 재발급 요청
        ResultActions result = mockMvc.perform(get("/api/auth/reissue")
                .header("Authorization", "Bearer " + refreshToken));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.message").value("새로운 access token 발급 완료"));
    }

    @Test
    @DisplayName("Access Token 재발급 실패 테스트 - Refresh Token 없음")
    public void reissue_fail_authentication() throws Exception {
        // when - 토큰 없이 요청
        ResultActions result = mockMvc.perform(get("/api/auth/reissue"));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.message").exists());
    }

//    @Test   //
//    @DisplayName("만료된 Access Token 요청 시 - 401 Unauthorized 응답")
//    public void expiredTokenTest() throws Exception {
//        // given: 이미 만료된 토큰 생성
//        Key key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
//
//        String expiredToken = Jwts.builder()
//                .setSubject("test@naver.com")
//                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)) // 2시간 전
//                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60))   // 1시간 전
//                .signWith(key, SignatureAlgorithm.HS512)
//                .compact();
//
//
//        // when, then: 토큰 인증 필요한 API 요청 시 401 반환 예상
//        mockMvc.perform(get("/api/auth/logout")
//                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.code").value(HttpStatus.UNAUTHORIZED.value()))
//                .andExpect(jsonPath("$.message").value("토큰 만료")); // 예시 메시지
//    }
}
