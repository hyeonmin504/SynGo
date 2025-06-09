package backend.synGo.controller.group;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.SlotPermission;
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

import static backend.synGo.controller.group.SlotMemberController.JoinMemberRequestForm;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SlotMemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired
    EntityManager em;

    private String leaderToken;
    private Long groupId;
    private Long slotId;
    private Long memberUserGroupId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 리더 회원가입 및 로그인
        signUp("leader@test.com", "리더");
        leaderToken = login("leader@test.com");

        // 2. 그룹 생성
        groupId = createGroup("테스트 그룹", leaderToken);

        // 3. 멤버 회원가입 및 로그인
        signUp("member@test.com", "멤버");
        String memberToken = login("member@test.com");

        // 4. 멤버 그룹 참여
        joinGroup(memberToken, groupId);

        // 5. 슬롯 생성
        slotId = createSlot(leaderToken, groupId);

        em.flush();
        em.clear();

        // 6. 멤버 userGroupId 조회
        memberUserGroupId = getUserGroupId(leaderToken, groupId);
    }

    @Test
    @DisplayName("슬롯 멤버 등록 성공")
    void registerSlotMember_success() throws Exception {
        JoinMemberRequestForm form = new JoinMemberRequestForm(memberUserGroupId, SlotPermission.BASIC);

        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(form))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 맴버 등록 성공"))
                .andExpect(jsonPath("$.data.slotId").value(slotId));
    }

    @Test
    @DisplayName("슬롯 멤버 등록 실패 - 비회원 접근")
    void registerSlotMember_fail_notMember() throws Exception {
        signUp("outsider@test.com", "외부인");
        String outsiderToken = login("outsider@test.com");

        JoinMemberRequestForm form = new JoinMemberRequestForm(memberUserGroupId, SlotPermission.EDITOR);

        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(form))))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("그룹원 외 접근 불가"));
    }

    @Test
    @DisplayName("슬롯 멤버 조회 성공 - 그룹원 접근 가능")
    void getSlotMember_success() throws Exception {
        // 1. 슬롯 멤버 등록
        JoinMemberRequestForm form = new JoinMemberRequestForm(memberUserGroupId, SlotPermission.BASIC);
        mockMvc.perform(post("/api/groups/" + groupId + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(form))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 맴버 등록 성공"));

        // 2. 그룹원이 슬롯 멤버 조회
        mockMvc.perform(get("/api/groups/" + groupId + "/slots/" + slotId + "/members")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 맴버 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nickname").exists())
                .andExpect(jsonPath("$.data[0].permission").exists());
    }

    @Test
    @DisplayName("그룹 전체 정보 맴버 조회")
    void getMemberInGroup_success() throws Exception {
        mockMvc.perform(get("/api/groups/" + groupId + "/member")
                        .header("Authorization", "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    // ======= 헬퍼 메서드 ========
    private void signUp(String email, String name) throws Exception {
        SignUpForm signUpForm = new SignUpForm(name, email, "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpForm)))
                .andExpect(status().isOk());
    }

    private String login(String email) throws Exception {
        LoginForm loginForm = new LoginForm("Qwer1234!", email);
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.data.accessToken");
    }

    private Long createGroup(String groupName, String token) throws Exception {
        GroupRequestForm form = GroupRequestForm.builder()
                .groupName(groupName)
                .nickname("리더닉")
                .info("그룹 설명")
                .password("pw1234")
                .checkPassword("pw1234")
                .groupType(GroupType.BASIC)
                .build();

        String response = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andReturn().getResponse().getContentAsString();
        Integer read = JsonPath.read(response, "$.data.groupId");
        return read.longValue();
    }

    private void joinGroup(String token, Long groupId) throws Exception {
        JoinGroupForm joinForm = JoinGroupForm.builder().password("pw1234").build();
        mockMvc.perform(post("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk());
    }

    private Long createSlot(String token, Long groupId) throws Exception {
        SlotForm form = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.PLAN)
                .title("회의 준비")
                .content("회의 자료 준비")
                .place("회의실 A")
                .importance(SlotImportance.MEDIUM)
                .build();

        String response = mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andReturn().getResponse().getContentAsString();
        Integer read = JsonPath.read(response, "$.data.slotId");
        return read.longValue();
    }

    private Long getUserGroupId(String token, Long groupId) throws Exception {
        String resp = mockMvc.perform(get("/api/groups/" + groupId + "/member")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> members = JsonPath.read(resp, "$.data");
        for (Map<String, Object> member : members) {
            if (!"LEADER".equals(member.get("role"))) {
                return ((Number) member.get("userGroupId")).longValue();
            }
        }
        throw new IllegalStateException("멤버 userGroupId 를 찾을 수 없습니다.");
    }
}