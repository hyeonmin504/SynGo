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
import lombok.extern.slf4j.Slf4j;
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

import static backend.synGo.controller.group.GroupBasicController.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupBasicControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager em;

    private String accessToken;
    private Long createdGroupId;
    private Long publicCreatedGroupId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. 회원가입
        SignUpForm signUpForm = SignUpForm.builder()
                .email("group@test.com")
                .name("GroupUser")
                .password("Qwer1234!")
                .checkPassword("Qwer1234!")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpForm)))
                .andExpect(status().isOk());

        // 2. 로그인
        LoginForm loginForm = LoginForm.builder()
                .email("group@test.com")
                .password("Qwer1234!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        accessToken = JsonPath.read(loginResponse, "$.data.accessToken");

        // 3. 그룹 생성
        GroupRequestForm requestForm = GroupRequestForm.builder()
                .groupName("동아리모임")
                .password("group1234")
                .checkPassword("group1234")
                .nickname("nickname")
                .info("동아리 정보")
                .groupType(GroupType.BASIC)
                .build();

        // 3. 공개 그룹 생성
        GroupRequestForm publicRequestForm = GroupRequestForm.builder()
                .groupName("동아리모임")
                .password("group1234")
                .checkPassword("group1234")
                .nickname("nickname")
                .info("동아리 정보")
                .groupType(GroupType.BASIC)
                .build();

        String groupResponse = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.groupId").exists())
                .andReturn().getResponse().getContentAsString();

        String publicGroupResponse = mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publicRequestForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.groupId").exists())
                .andReturn().getResponse().getContentAsString();


        Integer id = JsonPath.read(groupResponse, "$.data.groupId");
        createdGroupId = id.longValue();

        Integer publicId = JsonPath.read(groupResponse, "$.data.groupId");
        publicCreatedGroupId = id.longValue();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("그룹 생성 성공")
    void createGroup_success() throws Exception {
        // 3. 그룹 생성 요청
        GroupRequestForm requestForm = GroupRequestForm.builder()
                .groupName("동아리모임")
                .password("group1234")
                .checkPassword("group1234")
                .nickname("nickname")
                .info("동아리 정보")
                .groupType(GroupType.BASIC)
                .build();

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.groupId").exists());
    }

    @Test
    @DisplayName("특정 그룹 조회 성공")
    void getGroup_success() throws Exception {
        mockMvc.perform(get("/api/groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("정보 요청 성공"))
                .andExpect(jsonPath("$.data.groupId").value(createdGroupId))
                .andExpect(jsonPath("$.data.name").value("동아리모임"))
                .andExpect(jsonPath("$.data.nickname").value("nickname"))
                .andExpect(jsonPath("$.data.information").value("동아리 정보"))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @DisplayName("비공개 그룹 참여 성공")
    @Test
    void joinGroup_private_success() throws Exception {
        // 가입자 회원가입
        SignUpForm joiner = SignUpForm.builder()
                .email("member@test.com")
                .name("Member")
                .password("Qwer1234!")
                .checkPassword("Qwer1234!")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joiner)))
                .andExpect(status().isOk());

        // 가입자 로그인
        LoginForm loginForm = LoginForm.builder()
                .email("member@test.com")
                .password("Qwer1234!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        String memberToken = JsonPath.read(loginResponse, "$.data.accessToken");

        // 그룹 참여 요청
        JoinGroupForm joinForm = JoinGroupForm.builder()
                .password("group1234")
                .build();

        mockMvc.perform(post("/api/groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 참여 성공"));
    }

    @DisplayName("공개 그룹 참여 성공")
    @Test
    void joinGroup_success() throws Exception {
        // 가입자 회원가입
        SignUpForm joiner = SignUpForm.builder()
                .email("member@test.com")
                .name("Member")
                .password("Qwer1234!")
                .checkPassword("Qwer1234!")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joiner)))
                .andExpect(status().isOk());

        // 가입자 로그인
        LoginForm loginForm = LoginForm.builder()
                .email("member@test.com")
                .password("Qwer1234!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        String memberToken = JsonPath.read(loginResponse, "$.data.accessToken");

        // 그룹 참여 요청
        JoinGroupForm joinForm = JoinGroupForm.builder()
                .password("group1234")
                .build();

        mockMvc.perform(post("/api/groups/" + publicCreatedGroupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 참여 성공"));
    }

    @Test
    @DisplayName("가입된 사용자 그룹 참여 실패")
    void joinGroup_fail() throws Exception {
        // 그룹 참여 요청 (비공개 그룹 비밀번호 포함)
        JoinGroupForm joinGroupForm = JoinGroupForm.builder()
                .password("group1234")
                .build();

        mockMvc.perform(post("/api/groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinGroupForm)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("이미 가입된 회원입니다."));
    }

    @Test
    @DisplayName("그룹 역할 조회 성공")
    void getGroupRoles_success() throws Exception {
        mockMvc.perform(get("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 역할 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].nickname").value("nickname"))
                .andExpect(jsonPath("$.data[0].role").value("LEADER"));
    }

    @Test
    @DisplayName("그룹 역할 조회 실패 - 권한 없는 사용자")
    void getGroupRoles_fail_not_member() throws Exception {
        // outsider 회원가입 및 로그인
        SignUpForm outsiderSignUp = SignUpForm.builder()
                .email("outsider@test.com")
                .name("외부인")
                .password("Qwer1234!")
                .checkPassword("Qwer1234!")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsiderSignUp)))
                .andExpect(status().isOk());

        LoginForm outsiderLogin = LoginForm.builder()
                .email("outsider@test.com")
                .password("Qwer1234!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsiderLogin)))
                .andReturn().getResponse().getContentAsString();

        String outsiderToken = JsonPath.read(loginResponse, "$.data.accessToken");

        // outsider가 그룹 역할 조회 시도
        mockMvc.perform(get("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("접근이 불가능한 정보입니다."));
    }

    @Test
    @DisplayName("그룹 역할 수정 성공 - LEADER가 멤버를 MANAGER로")
    void updateGroupRole_success() throws Exception {
        // 1. 새로운 멤버 회원가입 및 참여
        String memberToken = registerAndJoinMember("member2@test.com", "멤버2", "Qwer1234!", createdGroupId);

        // 2. LEADER가 그룹 역할 조회해서 userGroupId 확인
        String response = mockMvc.perform(get("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> members = JsonPath.read(response, "$.data");
        Long targetUserGroupId = null;
        for (Map<String, Object> m : members) {
            if (!"LEADER".equals(m.get("role"))) {
                targetUserGroupId = ((Number) m.get("id")).longValue();
                break;
            }
        }

        // 3. 역할 수정 요청
        UserGroupRoleSummary request = new UserGroupRoleSummary(targetUserGroupId, "멤버2", Role.MANAGER);

        mockMvc.perform(post("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 역할 수정 성공"));
    }

    @Test
    @DisplayName("그룹 역할 수정 성공 - 리더 위임")
    void updateGroupRole_delegate_leader_success() throws Exception {
        // 1. 새로운 멤버 회원가입 및 참여
        String memberToken = registerAndJoinMember("member2@test.com", "멤버2", "Qwer1234!", createdGroupId);

        // 2. 역할 조회로 userGroupId 확보 (멤버2의 ID)
        String response = mockMvc.perform(get("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> members = JsonPath.read(response, "$.data");
        Long leaderId = null;
        Long member2Id = null;

        for (Map<String, Object> m : members) {
            Long id = ((Number) m.get("id")).longValue();
            String role = (String) m.get("role");

            if ("LEADER".equals(role)) leaderId = id;
            else member2Id = id;
        }

        // 위임
        UserGroupRoleSummary delegate = new UserGroupRoleSummary(member2Id, "임의닉네임", Role.LEADER);
        mockMvc.perform(post("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(delegate))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 역할 수정 성공"));

        // 4. 변경된 역할 다시 조회
        String updated = mockMvc.perform(get("/api/groups/" + createdGroupId + "/role")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 역할 조회 성공"))
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> updatedMembers = JsonPath.read(updated, "$.data");

        boolean newLeaderConfirmed = false;
        boolean oldLeaderIsMember = false;

        for (Map<String, Object> m : updatedMembers) {
            Long id = ((Number) m.get("id")).longValue();
            String role = (String) m.get("role");

            if (id.equals(member2Id) && role.equals("LEADER")) newLeaderConfirmed = true;
            if (id.equals(leaderId) && role.equals("MEMBER")) oldLeaderIsMember = true;
        }
        // 최종 검증
        assert newLeaderConfirmed : "멤버2가 리더가 되어야 합니다.";
        assert oldLeaderIsMember : "기존 리더는 MEMBER로 강등되어야 합니다.";
    }

    private String registerAndJoinMember(String email, String name, String userPassword, Long groupId) throws Exception {
        String groupPassword = "group1234"; // 그룹 생성 시 지정한 비밀번호

        // 회원가입
        SignUpForm signUpForm = SignUpForm.builder()
                .email(email)
                .name(name)
                .password(userPassword)
                .checkPassword(userPassword)
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpForm)))
                .andExpect(status().isOk());

        // 로그인
        LoginForm loginForm = LoginForm.builder()
                .email(email)
                .password(userPassword)
                .build();

        String loginResp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(loginResp, "$.data.accessToken");

        // 그룹 참여
        JoinGroupForm joinForm = JoinGroupForm.builder()
                .password(groupPassword) // ← 여기 주의!
                .build();

        mockMvc.perform(post("/api/groups/" + groupId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinForm)))
                .andExpect(status().isOk());

        em.flush();
        em.clear();
        return token;
    }
}