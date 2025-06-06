package backend.synGo.controller;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.domain.user.User;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.repository.UserRepository;
import backend.synGo.repository.UserSchedulerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MySlotControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSchedulerRepository userSchedulerRepository;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 등록
        UserScheduler scheduler = new UserScheduler(Theme.BLACK);
        UserScheduler savedScheduler = userSchedulerRepository.save(scheduler);
        User user = new User(
                "SlotUser",
                "slot@test.com",
                passwordEncoder.encode("Qwer1234!"),
                "127.0.0.1",
                savedScheduler
        );
        userRepository.save(user);

        // 로그인 → AccessToken 획득
        LoginForm loginForm = LoginForm.builder()
                .email("slot@test.com")
                .password("Qwer1234!")
                .build();

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        accessToken = JsonPath.read(response, "$.data.accessToken");
    }

    @Test
    @DisplayName("슬롯 생성 실패 - 날짜 유효성 실패")
    void generateSlotFail_dueToInvalidDate() throws Exception {
        SlotForm invalidForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(2)) // 시작일이 종료일보다 늦음
                .endDate(LocalDateTime.now().plusDays(1))
                .status(Status.PLAN)
                .title("Invalid Date Test")
                .content("잘못된 날짜 테스트")
                .place("카페")
                .importance(SlotImportance.MEDIUM)
                .build();

        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(invalidForm)))
                .andExpect(status().isOk()) // 406 또는 실제 API 응답에 맞게 수정
                .andExpect(jsonPath("$.message").value("날자를 확인해주세요.")); // 실제 예외 메시지로 맞추기
    }

    @Test
    @DisplayName("슬롯 생성 성공")
    void generateSlotSuccess() throws Exception {
        SlotForm validForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.DELAY)
                .title("Valid Slot")
                .content("정상적인 슬롯 생성")
                .place("회의실")
                .importance(SlotImportance.HIGH)
                .build();

        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(validForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 생성 성공"));
    }
}