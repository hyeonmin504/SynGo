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
import java.util.List;
import java.util.Map;

import static backend.synGo.controller.group.SlotMemberController.*;
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
    private Long slotId;

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

        // 슬롯 생성
        SlotForm validForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.PLAN)
                .title("회의 준비")
                .content("회의 자료 준비")
                .place("회의실 B")
                .importance(SlotImportance.MEDIUM)
                .build();

        String slotCreateResp = mockMvc.perform(post("/api/groups/"+ groupId + "/slots")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validForm)))
                .andReturn().getResponse().getContentAsString();

        Integer id2 = JsonPath.read(slotCreateResp, "$.data.slotId");
        slotId = id2.longValue();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("슬롯 생성 성공")
    void createGroupSlot_success() throws Exception {
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
    void createGroupSlot_auth_fail() throws Exception {
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

    @Test
    @DisplayName("슬롯 조회 성공 - 그룹원은 조회 가능")
    void getGroupSlot_success() throws Exception {
        // 생성한 슬롯을 그룹원이 조회
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/groups/" + groupId + "/slots/" + slotId)
                                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.slotId").value(slotId))
                .andExpect(jsonPath("$.message").value("슬롯 요청 성공"));
    }

    @Test
    @DisplayName("슬롯 조회 실패 - 그룹원이 아닌 유저는 조회 불가")
    void getGroupSlot_notGroupMember() throws Exception {
        // outsider 유저 생성 및 로그인
        SignUpForm outsiderSignUp = new SignUpForm("비회원", "outsider@test.com", "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsiderSignUp)))
                .andExpect(status().isOk());

        LoginForm outsiderLogin = new LoginForm("Qwer1234!", "outsider@test.com");
        String outsiderLoginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsiderLogin)))
                .andReturn().getResponse().getContentAsString();
        String outsiderToken = JsonPath.read(outsiderLoginResp, "$.data.accessToken");

        // outsider가 슬롯 조회 시도 → 실패
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/groups/" + groupId + "/slots/" + slotId)
                                .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("그룹원 외 접근 불가"));
    }

    @Test
    @DisplayName("슬롯 상태 수정 성공 - 에디터 권한")
    void updateSlotStatus_success_editor() throws Exception {
        // 그룹장이 멤버를 SLOT 에디터로 지정
        Long userGroupId = getUserGroupId(memberToken);
        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"userGroupId\":" + userGroupId + ",\"permission\":\"EDITOR\"}]"))
                .andExpect(status().isOk());

        GroupSlotStatusForm form = new GroupSlotStatusForm();
        form.status = Status.DELAY;

        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 상태 업데이트 성공"));
    }

    @Test
    @DisplayName("슬롯 상태 수정 실패 - BASIC 권한")
    void updateSlotStatus_fail_basic() throws Exception {
        GroupSlotStatusForm form = new GroupSlotStatusForm();
        form.status = Status.PLAN;

        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("변경 권한이 없습니다"));
    }

    private Long getUserGroupId(String token) throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> members = JsonPath.read(response, "$.data");
        for (Map<String, Object> m : members) {
            if (!"LEADER".equals(m.get("role"))) {
                return ((Number) m.get("id")).longValue();
            }
        }
        return null;
    }

}