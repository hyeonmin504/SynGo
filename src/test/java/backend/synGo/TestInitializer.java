package backend.synGo;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@TestConfiguration
public class TestInitializer {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager em;

    public TestScenario createBasicScenario() throws Exception {
        TestScenario scenario = new TestScenario();

        // 1. 리더 회원가입 및 로그인
        signUp("leader@test.com", "리더");
        scenario.leaderToken = login("leader@test.com");

        // 2. 그룹 생성
        scenario.groupId = createGroup("테스트 그룹", scenario.leaderToken);

        // 3. 멤버 회원가입 및 로그인
        signUp("member@test.com", "멤버");
        scenario.memberToken = login("member@test.com");

        // 4. 멤버 그룹 참여
        joinGroup(scenario.memberToken, scenario.groupId);

        // 5. 리더가 슬롯 생성
        scenario.slotId = createSlot(scenario.leaderToken, scenario.groupId);

        em.flush();
        em.clear();

        // 6. 멤버 userGroupId 조회 (member의 토큰 사용)
        scenario.memberUserGroupId = getUserGroupId(scenario.memberToken, scenario.groupId);

        return scenario;
    }

    public void signUp(String email, String name) throws Exception {
        SignUpForm signUpForm = new SignUpForm(name, email, "Qwer1234!", "Qwer1234!");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpForm)))
                .andExpect(status().isOk());
    }

    public String login(String email) throws Exception {
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
        SlotForm slotForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.PLAN)
                .title("회의 준비")
                .content("회의 자료 준비")
                .place("회의실 A")
                .importance(SlotImportance.MEDIUM)
                .build();

        String resp = mockMvc.perform(post("/api/groups/" + groupId + "/slots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotForm)))
                .andReturn().getResponse().getContentAsString();

        Integer read = JsonPath.read(resp, "$.data.slotId");
        return read.longValue();
    }

    public Long getUserGroupId(String token, Long groupId) throws Exception {
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