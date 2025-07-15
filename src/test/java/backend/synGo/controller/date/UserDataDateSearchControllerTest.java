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
import java.util.List;
import java.util.Map;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private Long groupId2;
    /**
     * 개인 슬롯 내일 3개 모래 1개
     * 그룹1 슬롯 내일 1개 - 에디터 할당
     * 그룹2 슬롯 내일 2개 모래 1개
     * === 결과 예상 ===
     * 개인 월 슬롯 데이터 조회(이번 달) - date.size() == 2 && date[].slotCount == 4개, 1개 존재 && date[].slotInfo.size == 2개, 1개
     * 개인 월 그룹 슬롯 데이터 조회(이번 달) - date.size == 2 && date[].slotCount == 3개, 1개 존재 && date[].slotInfo.size == 2개, 1개
     * 개인 일 슬롯 데이터 조회(내일) -  date.slotCount == 3
     * 개일 일 그룹 슬롯 데이터 조회(내일) - date.slotCount == 3
     */
    @Test
    @DisplayName("개인 월 슬롯 데이터 조회 성공 - date.size == 2, slotCount 각각 3, 1, slotInfo.size 각각 2, 1")
    void getUserDataForMonth_success() throws Exception {
        mockMvc.perform(get("/api/my/date/month")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("개인 데이터 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].slotCount").value(3))
                .andExpect(jsonPath("$.data[0].slotInfo", hasSize(2)))
                .andExpect(jsonPath("$.data[1].slotCount").value(1))
                .andExpect(jsonPath("$.data[1].slotInfo", hasSize(1)));
    }

    @Test
    @DisplayName("개인 월 그룹 슬롯 데이터 조회 성공 - date.size == 2, slotCount 각각 3, 1, slotInfo.size 각각 2, 1")
    void getUserByGroupDataForMonth_success() throws Exception {
        mockMvc.perform(get("/api/my/groups/date/month")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("개인의 그룹 데이터 조회 성공"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].slotCount").value(3))
                .andExpect(jsonPath("$.data[0].slotInfo", hasSize(2)))
                .andExpect(jsonPath("$.data[1].slotCount").value(1))
                .andExpect(jsonPath("$.data[1].slotInfo", hasSize(1)));
    }

    @Test
    @DisplayName("개인 내일 슬롯 데이터 조회 성공 - slotCount == 3, slotInfo.size == 3")
    void getUserDataForDay_success() throws Exception {
        mockMvc.perform(get("/api/my/date/day")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .param("day", String.valueOf(LocalDate.now().getDayOfMonth() + 1))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.slotCount").value(3))
                .andExpect(jsonPath("$.data.slotInfo", hasSize(3)));
    }

    @Test
    @DisplayName("개인 내일 모래 슬롯 데이터 조회 성공 - slotCount == 3, slotInfo.size == 3")
    void getUserDataForDay_success2() throws Exception {
        mockMvc.perform(get("/api/my/date/day")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .param("day", String.valueOf(LocalDate.now().getDayOfMonth() + 2))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.slotCount").value(1))
                .andExpect(jsonPath("$.data.slotInfo", hasSize(1)));
    }

    @Test
    @DisplayName("개인 일 그룹 슬롯 데이터 조회 성공 - slotCount == 3, slotInfo.size == 3")
    void getUserByGroupDataForDay_success() throws Exception {
        mockMvc.perform(get("/api/my/groups/date/day")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .param("day", String.valueOf(LocalDate.now().getDayOfMonth() + 1))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.slotCount").value(3))
                .andExpect(jsonPath("$.data.slotInfo", hasSize(3)));
    }

    @Test
    @DisplayName("개인 일 그룹 슬롯 데이터 조회 성공 - slotCount == 3, slotInfo.size == 3")
    void getUserByGroupDataForDay_success2() throws Exception {
        mockMvc.perform(get("/api/my/groups/date/day")
                        .param("year", String.valueOf(LocalDate.now().getYear()))
                        .param("month", String.valueOf(LocalDate.now().getMonthValue()))
                        .param("day", String.valueOf(LocalDate.now().getDayOfMonth() + 2))
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.slotCount").value(1))
                .andExpect(jsonPath("$.data.slotInfo", hasSize(1)));
    }

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

        //개인 슬롯 1번 - 시작 시간: 내일
        SlotForm mySlot = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.DELAY)
                .title("Valid Slot1-1")
                .content("정상적인 슬롯 생성1-1")
                .place("회의실1-1")
                .importance(SlotImportance.LOW)
                .build();
        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + leaderToken)
                        .content(objectMapper.writeValueAsString(mySlot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 생성 성공"));
        //개인 슬롯 2번 - 시작 시간: 내일
        SlotForm mySlot2 = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.DELAY)
                .title("Valid Slot1-2")
                .content("정상적인 슬롯 생성1-2")
                .place("회의실1-2")
                .importance(SlotImportance.HIGH)
                .build();
        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + leaderToken)
                        .content(objectMapper.writeValueAsString(mySlot2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 생성 성공"));
        //개인 슬롯 3번 - 시작 시간: 내일
        SlotForm mySlot3 = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.DELAY)
                .title("Valid Slot1-3")
                .content("정상적인 슬롯 생성1-3")
                .place("회의실1-3")
                .importance(SlotImportance.HIGH)
                .build();
        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + leaderToken)
                        .content(objectMapper.writeValueAsString(mySlot3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 생성 성공"));
        //개인 슬롯 4번 - 시작 시간: 2일 뒤
        SlotForm mySlot4 = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(3))
                .status(Status.DELAY)
                .title("Valid Slot2-4")
                .content("정상적인 슬롯 생성2-4")
                .place("회의실2-4")
                .importance(SlotImportance.HIGH)
                .build();
        mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + leaderToken)
                        .content(objectMapper.writeValueAsString(mySlot4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 생성 성공"));

        // 2. 그룹 생성 1번
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
        //그룹 2번 생성
        GroupRequestForm form2 = GroupRequestForm.builder()
                .groupName("일정 테스트 그룹")
                .nickname("리더닉")
                .info("테스트 설명")
                .password("pw1234")
                .checkPassword("pw1234")
                .groupType(GroupType.BASIC)
                .build();
        String createResp2 = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form2)))
                .andReturn().getResponse().getContentAsString();
        Integer read2 = JsonPath.read(createResp2, "$.data.groupId");
        groupId2 = read2.longValue();

        em.flush();
        em.clear();

        //2번 그룹 슬롯 1번 - 시작 시간: 내일
        SlotForm slotForm = SlotForm.builder()
                .title("회의2-1")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .place("회의실2-1")
                .content("회의2-1")
                .status(Status.PLAN)
                .importance(SlotImportance.HIGH)
                .build();
        String slotRes = mockMvc.perform(post("/api/groups/" + groupId2 + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm)))
                .andReturn().getResponse().getContentAsString();
        Integer readId = JsonPath.read(slotRes, "$.data.slotId");
        long slotId = readId.longValue();

        em.flush();
        em.clear();

        // 그룹장이 자신을 SLOT 에디터로 지정
        Long userGroupId = getUserGroupId(leaderToken);
        mockMvc.perform(post("/api/groups/" + groupId2 + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"userGroupId\":" + userGroupId + ",\"permission\":\"EDITOR\"}]"))
                .andExpect(status().isOk());

        //1번 그룹 슬롯 1번 - 시작 시간: 내일
        SlotForm slotForm2 = SlotForm.builder()
                .title("회의1-1")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .place("회의실1-1")
                .content("회의 준비1-1")
                .status(Status.PLAN)
                .importance(SlotImportance.LOW)
                .build();
        String slotRes2 = mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm2)))
                .andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
        //1번 그룹 슬롯 2번 - 시작 시간: 내일
        SlotForm slotForm3 = SlotForm.builder()
                .title("회의1-2")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .place("회의1-2")
                .content("회의 준비1-2")
                .status(Status.PLAN)
                .importance(SlotImportance.LOW)
                .build();
        String slotRes3 = mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm3)))
                .andReturn().getResponse().getContentAsString();
        //1번 그룹 슬롯 3번 - 시작 시간: 내일 모래
        SlotForm slotForm4 = SlotForm.builder()
                .title("회의1-3")
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(2).plusHours(1))
                .place("회의실 A1-3")
                .content("회의 준비1-3")
                .status(Status.PLAN)
                .importance(SlotImportance.LOW)
                .build();
        String slotRes4 = mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm4)))
                .andReturn().getResponse().getContentAsString();
        em.flush();
        em.clear();
    }

    private Long getUserGroupId(String token) throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/groups/" + groupId2 + "/role")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> members = JsonPath.read(response, "$.data");
        for (Map<String, Object> m : members) {
            if ("LEADER".equals(m.get("role"))) {
                return ((Number) m.get("id")).longValue();
            }
        }
        return null;
    }
}