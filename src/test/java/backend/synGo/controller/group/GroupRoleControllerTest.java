package backend.synGo.controller.group;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupRoleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired
    EntityManager em;

    private String leaderToken;
    private String memberToken;
    private Long groupId;
    private Long memberUserGroupId;

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
        // 그룹원 전체 조회해서 멤버의 userGroupId 추출
        String rolesJson = mockMvc.perform(get("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + leaderToken))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> members = JsonPath.read(rolesJson, "$.data");

        for (Map<String, Object> member : members) {
            if (!"LEADER".equals(member.get("role"))) {
                memberUserGroupId = ((Number) member.get("id")).longValue();
                break;
            }
        }
    }

    @Test
    @DisplayName("LEADER가 그룹원의 역할을 성공적으로 변경")
    void leaderCanUpdateRoles() throws Exception {
        List<GroupBasicController.UserGroupRoleSummary> updateList = List.of(
                new GroupBasicController.UserGroupRoleSummary(memberUserGroupId, "멤버", Role.MANAGER)
        );

        mockMvc.perform(post("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateList)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 역할 수정 성공"));
    }

    @Test
    @DisplayName("LEADER가 LEADER를 중복 지정하면 예외 발생")
    void updateMembersRole_Duplicate_fail() throws Exception {
        List<GroupBasicController.UserGroupRoleSummary> updateList = List.of(
                new GroupBasicController.UserGroupRoleSummary(memberUserGroupId, "멤버", Role.LEADER),
                new GroupBasicController.UserGroupRoleSummary(memberUserGroupId, "멤버", Role.LEADER)
        );

        mockMvc.perform(post("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateList)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("이미 새 리더가 존재합니다"));
    }

    @Test
    @DisplayName("MANAGER가 LEADER로 역할 변경 시도 시 거부됨")
    void updateMembersRole_manager_fail() throws Exception {
        // 우선 LEADER가 멤버를 MANAGER로 승격
        List<GroupBasicController.UserGroupRoleSummary> promote = List.of(
                new GroupBasicController.UserGroupRoleSummary(memberUserGroupId, "멤버", Role.MANAGER)
        );
        mockMvc.perform(post("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promote)))
                .andExpect(status().isOk());

        // 멤버가 LEADER로 변경 시도 → 실패해야 함
        List<GroupBasicController.UserGroupRoleSummary> toLeader = List.of(
                new GroupBasicController.UserGroupRoleSummary(memberUserGroupId, "멤버", Role.LEADER)
        );

        mockMvc.perform(post("/api/groups/" + groupId + "/role")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toLeader)))
                .andExpect(status().isNotAcceptable());
    }
}