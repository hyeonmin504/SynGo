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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupDateSearchControllerTest {

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

    @Test
    @DisplayName("그룹 날짜 조회 성공")
    void getGroupDateData_success() throws Exception {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        mockMvc.perform(get("/api/groups/" + groupId + "/date/month")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(groupId))
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("존재하지 않는 월 조회 시 기본값 적용")
    void getGroupDateData_withDefaultValues() throws Exception {
        mockMvc.perform(get("/api/groups/" + groupId + "/date/month")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("유효하지 않은 year/month 파라미터 검증 실패")
    void getGroupDateData_invalidParams() throws Exception {
        mockMvc.perform(get("/api/groups/" + groupId + "/date/month")
                        .param("year", "1999")
                        .param("month", "13")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("하루 슬롯 조회 성공")
    void getGroupDayDateData_success() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/api/groups/" + groupId + "/date/day")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("year", String.valueOf(tomorrow.getYear()))
                        .param("month", String.valueOf(tomorrow.getMonthValue()))
                        .param("day", String.valueOf(tomorrow.getDayOfMonth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today").value(tomorrow.toString()))
                .andExpect(jsonPath("$.data.slotCount").value(1))
                .andExpect(jsonPath("$.data.slotInfo[0].title").value("회의"))
                .andExpect(jsonPath("$.data.slotInfo[0].editorNickname").doesNotExist())
                .andExpect(jsonPath("$.message").value("조회 성공"));
    }

    @Test
    @DisplayName("하루 슬롯 조회 실패 - 잘못된 날짜")
    void getGroupDayDateData_invalidDay() throws Exception {
        mockMvc.perform(get("/api/groups/" + groupId + "/date/day")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("year", "2024")
                        .param("month", "2")
                        .param("day", "31")) // 2월 31일은 존재하지 않음
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("하루 슬롯 조회 실패 - 파라미터 범위 초과")
    void getGroupDayDateData_invalidParamRange() throws Exception {
        mockMvc.perform(get("/api/groups/" + groupId + "/date/day")
                        .header("Authorization", "Bearer " + leaderToken)
                        .param("year", "1500")
                        .param("month", "20")
                        .param("day", "50"))
                .andExpect(status().isBadRequest());
    }
}