package backend.synGo.controller.group;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.domain.group.GroupType;
import backend.synGo.form.requestForm.GroupRequestForm;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupBasicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

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
                .andExpect(jsonPath("$.data.groupId").exists());
    }
}