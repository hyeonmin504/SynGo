package backend.synGo.controller.group;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import backend.synGo.form.requestForm.SlotForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class GroupSlotControllerTest {

    @Autowired
    EntityManager em;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private String leaderToken;
    private Long groupId;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        // LEADER 회원가입 및 로그인
        SignUpForm leaderSignUp = new SignUpForm("셋업이름", "leader@test.com", "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaderSignUp)))
                .andExpect(status().isOk());

        LoginForm leaderLogin = new LoginForm("Qwer1234!","leader@test.com");
        String leaderLoginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaderLogin)))
                .andReturn().getResponse().getContentAsString();
        leaderToken = JsonPath.read(leaderLoginResp, "$.data.accessToken");

        // 그룹 생성
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName("테스트 그룹")
                .nickname("리더닉")
                .info("설명")
                .password("pw1234")
                .checkPassword("pw1234")
                .groupType(GroupType.BASIC)
                .build();

        String createResp = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(createResp, "$.data.groupId");
        groupId = id.longValue();

        // 멤버 회원가입 및 로그인
        SignUpForm memberSignUp = new SignUpForm("셋업이름멤버", "member@test.com", "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberSignUp)))
                .andExpect(status().isOk());

        LoginForm memberLogin = new LoginForm("Qwer1234!","member@test.com");
        String memberLoginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberLogin)))
                .andReturn().getResponse().getContentAsString();
        memberToken = JsonPath.read(memberLoginResp, "$.data.accessToken");

        // 멤버 그룹 참여
        JoinGroupForm joinForm = JoinGroupForm.builder().password("pw1234").build();
        mockMvc.perform(post("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
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

        mockMvc.perform(post("/api/groups/"+ groupId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + leaderToken)
                        .content(objectMapper.writeValueAsString(validForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 슬롯 등록 성공"));
    }

    @Test
    @DisplayName("슬롯 생성 실패 - 권환 없는 유저 접근")
    void generateSlotFail() throws Exception {
        SlotForm validForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.DELAY)
                .title("Valid Slot")
                .content("정상적인 슬롯 생성")
                .place("회의실")
                .importance(SlotImportance.HIGH)
                .build();

        mockMvc.perform(post("/api/groups/"+ groupId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + memberToken)
                        .content(objectMapper.writeValueAsString(validForm)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한 부족"));
    }
}