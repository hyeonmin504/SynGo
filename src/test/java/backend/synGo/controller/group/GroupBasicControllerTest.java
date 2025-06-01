package backend.synGo.controller.group;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupBasicControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;
    private Long createdUserGroupId;

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

        String groupResponse = mockMvc.perform(post("/api/groups/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.userGroupId").exists())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(groupResponse, "$.data.userGroupId");
        createdUserGroupId = id.longValue();
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

        mockMvc.perform(post("/api/groups/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 생성 성공"))
                .andExpect(jsonPath("$.data.userGroupId").exists());
    }

    @Test
    @DisplayName("특정 그룹 조회 성공")
    void getGroup_success() throws Exception {
        mockMvc.perform(get("/api/groups/" + createdUserGroupId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("정보 요청 성공"))
                .andExpect(jsonPath("$.data.userGroupId").value(createdUserGroupId))
                .andExpect(jsonPath("$.data.name").value("동아리모임"))
                .andExpect(jsonPath("$.data.nickname").value("nickname"))
                .andExpect(jsonPath("$.data.information").value("동아리 정보"))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("비공개 그룹 참여 성공")
    void joinGroup_success() throws Exception {
        //가입자 생성
        SignUpForm joiner = SignUpForm.builder()
                .email("member@test.com")
                .name("Member")
                .password("Qwer1234!")
                .checkPassword("Qwer1234!")
                .build();
        //가입자 회원가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joiner)))
                .andExpect(status().isOk());

        LoginForm loginForm = LoginForm.builder()
                .email("member@test.com")
                .password("Qwer1234!")
                .build();
        //가입자 로그인
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();
        String memberToken = JsonPath.read(loginResponse, "$.data.accessToken");

        JoinGroupForm joinGroupForm = JoinGroupForm.builder().password("group1234").build();

        mockMvc.perform(post("/api/groups/" + createdUserGroupId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinGroupForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("그룹 참여 성공"));
    }

    @Test
    @DisplayName("가입된 사용자 그룹 참여 실패")
    void joinPrivateGroup_success() throws Exception {
        // 그룹 참여 요청 (비공개 그룹 비밀번호 포함)
        JoinGroupForm joinGroupForm = JoinGroupForm.builder()
                .password("group1234")
                .build();

        mockMvc.perform(post("/api/groups/" + createdUserGroupId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinGroupForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이미 가입된 회원입니다."));
    }


}