package backend.synGo.controller.date;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.SlotForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserDataDateSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager em;

    private String leaderToken;
    private Long groupId;
    @BeforeEach
    void setUp() throws Exception {
        // 1. 회원가입 및 로그인
        SignUpForm signUpForm = new SignUpForm("테스트유저", "leader@test.com", "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpForm)))
                .andExpect(status().isOk());

        LoginForm loginForm = new LoginForm("Qwer1234!", "leader@test.com");
        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        leaderToken = JsonPath.read(loginResp, "$.data.accessToken");

        // 2. 그룹 생성
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName("일정 테스트 그룹")
                .nickname("리더닉")
                .info("테스트 설명")
                .password("pw1234")
                .checkPassword("pw1234")
                .groupType(GroupType.BASIC)
                .build();

        String createResp = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andReturn().getResponse().getContentAsString();

        Integer read = JsonPath.read(createResp, "$.data.groupId");
        groupId = read.longValue();

        // 3. 테스트용 슬롯 생성 (slot 생성으로 date도 생성됨)
        SlotForm slotForm = SlotForm.builder()
                .title("회의")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .place("회의실 A")
                .content("회의 준비")
                .status(Status.PLAN)
                .importance(SlotImportance.HIGH)
                .build();

        mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
    }


}