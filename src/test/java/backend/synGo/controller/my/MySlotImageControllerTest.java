package backend.synGo.controller.my;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.domain.schedule.Theme;
import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.domain.user.User;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.repository.UserRepository;
import backend.synGo.repository.UserSchedulerRepository;
import backend.synGo.service.ThemeService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static backend.synGo.controller.my.MySlotImageController.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MySlotImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSchedulerRepository userSchedulerRepository;

    @Autowired
    private ThemeService themeService;
    @Autowired
    EntityManager em;

    private String accessToken;
    private Long slotId;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 생성
        UserScheduler scheduler = new UserScheduler(themeService.getTheme(Theme.BLACK));
        UserScheduler savedScheduler = userSchedulerRepository.save(scheduler);
        testUser = new User(
                "ImageTestUser",
                "imagetest@test.com",
                passwordEncoder.encode("Qwer1234!"),
                "127.0.0.1",
                savedScheduler
        );
        userRepository.save(testUser);

        // 로그인하여 토큰 획득
        LoginForm loginForm = LoginForm.builder()
                .email("imagetest@test.com")
                .password("Qwer1234!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginForm)))
                .andReturn().getResponse().getContentAsString();

        accessToken = JsonPath.read(loginResponse, "$.data.accessToken");

        // 이미지 테스트를 위한 슬롯 생성
        SlotForm slotForm = SlotForm.builder()
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(Status.PLAN)
                .title("이미지 테스트 전용 슬롯")
                .content("이미지 업로드/조회/삭제 테스트용")
                .place("테스트 장소")
                .importance(SlotImportance.MEDIUM)
                .build();

        String slotResponse = mockMvc.perform(post("/api/my/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(slotForm)))
                .andReturn().getResponse().getContentAsString();

        Integer readSlotId = JsonPath.read(slotResponse, "$.data.slotId");
        slotId = readSlotId.longValue();
    }

    @Test
    @DisplayName("내 슬롯 이미지 조회 성공 - 이미지 있는 경우")
    void getMySlotImages_Success_WithImages() throws Exception {
        // 1. 먼저 이미지 업로드
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.png",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", slotId)
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk());

        // 2. 이미지 조회
        mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("슬롯 이미지 요청 성공"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls").isNotEmpty());
    }

    @Test
    @DisplayName("내 슬롯 이미지 조회 실패 - 다른 유저의 슬롯 접근 시도")
    void getMySlotImages_Fail_AccessDenied() throws Exception {
        // 다른 유저 생성
        UserScheduler otherScheduler = new UserScheduler(themeService.getTheme(Theme.BLACK));
        UserScheduler savedOtherScheduler = userSchedulerRepository.save(otherScheduler);
        User otherUser = new User(
                "OtherUser",
                "other@test.com",
                passwordEncoder.encode("Qwer1234!"),
                "127.0.0.1",
                savedOtherScheduler
        );
        userRepository.save(otherUser);

        // 다른 유저로 로그인
        LoginForm otherLoginForm = LoginForm.builder()
                .email("other@test.com")
                .password("Qwer1234!")
                .build();

        String otherLoginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLoginForm)))
                .andReturn().getResponse().getContentAsString();

        String otherAccessToken = JsonPath.read(otherLoginResponse, "$.data.accessToken");

        // 다른 유저의 토큰으로 접근 시도
        mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + otherAccessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 유저의 슬롯이 아닙니다."));
    }

    @Test
    @DisplayName("내 슬롯 이미지 조회 실패 - 존재하지 않는 슬롯")
    void getMySlotImages_Fail_NotFoundSlot() throws Exception {
        Long nonExistentSlotId = 99999L;

        mockMvc.perform(get("/api/my/slots/{slotId}/images", nonExistentSlotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.imageUrls").isEmpty()); // 빈 배열 체크
    }

    @Test
    @DisplayName("내 슬롯 이미지 업로드 성공 - 단일 이미지")
    void uploadMySlotImage_Success_SingleImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test-image.png",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", slotId)
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이미지 등록 성공"))
                .andExpect(jsonPath("$.data.slotId").value(slotId));
    }

    @Test
    @DisplayName("내 슬롯 이미지 업로드 성공 - 다중 이미지")
    void uploadMySlotImage_Success_MultipleImages() throws Exception {
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "test1.png",
                MediaType.IMAGE_JPEG_VALUE,
                "test image 1".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "test2.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image 2".getBytes()
        );

        MockMultipartFile image3 = new MockMultipartFile(
                "images",
                "test3.png",
                MediaType.IMAGE_JPEG_VALUE,
                "test image 3".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", slotId)
                        .file(image1)
                        .file(image2)
                        .file(image3)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이미지 등록 성공"))
                .andExpect(jsonPath("$.data.slotId").value(slotId));
    }

    @Test
    @DisplayName("내 슬롯 이미지 업로드 실패 - 존재하지 않는 슬롯")
    void uploadMySlotImage_Fail_NotFoundSlot() throws Exception {
        Long nonExistentSlotId = 99999L;

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.png",
                MediaType.IMAGE_JPEG_VALUE,
                "test image".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", nonExistentSlotId)
                        .file(image)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("내 슬롯 이미지 삭제 성공")
    void deleteMySlotImage_Success() throws Exception {
        // 1. 먼저 이미지 업로드
        MockMultipartFile uploadImage = new MockMultipartFile(
                "images",
                "delete-test.png",
                MediaType.IMAGE_JPEG_VALUE,
                "image to be deleted".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", slotId)
                        .file(uploadImage)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk());

        // 2. 업로드된 이미지 URL 조회
        String getResponse = mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> imageUrls = JsonPath.read(getResponse, "$.data.imageUrls");

        em.flush();
        em.clear();

        // 3. 이미지 삭제
        UserSlotImageUrlForm deleteForm = new UserSlotImageUrlForm();
        deleteForm.setImageUrls(imageUrls);

        mockMvc.perform(delete("/api/my/slots/{slotId}/images", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(deleteForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이미지 삭제 성공"));

        // 4. 삭제 확인 (이미지가 비어있는지 확인)
        mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls").isEmpty());
    }

    @Test
    @DisplayName("내 슬롯 이미지 부분 삭제 성공")
    void deleteMySlotImage_PartialDelete_Success() throws Exception {
        // 1. 여러 이미지 업로드
        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "keep.png",
                MediaType.IMAGE_JPEG_VALUE,
                "image to keep".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "delete.png",
                MediaType.IMAGE_JPEG_VALUE,
                "image to delete".getBytes()
        );

        mockMvc.perform(multipart("/api/my/slots/{slotId}/images", slotId)
                        .file(image1)
                        .file(image2)
                        .header("Authorization", "Bearer " + accessToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk());

        em.flush();
        em.clear();

        // 2. 업로드된 이미지 URL 조회
        String getResponse = mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> allImageUrls = JsonPath.read(getResponse, "$.data.imageUrls");

        // 3. 첫 번째 이미지만 삭제
        UserSlotImageUrlForm deleteForm = new UserSlotImageUrlForm();
        deleteForm.setImageUrls(Arrays.asList(allImageUrls.get(0)));

        mockMvc.perform(delete("/api/my/slots/{slotId}/images", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(deleteForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이미지 삭제 성공"));

        // 4. 남은 이미지 확인
        mockMvc.perform(get("/api/my/slots/{slotId}/images", slotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls.length()").value(1));
    }

    @Test
    @DisplayName("내 슬롯 이미지 삭제 실패 - 존재하지 않는 이미지 URL")
    void deleteMySlotImage_Fail_NotFoundImage() throws Exception {
        UserSlotImageUrlForm deleteForm = new UserSlotImageUrlForm();
        deleteForm.setImageUrls(Arrays.asList(
                "http://fake-url.com/non-existent1.png",
                "http://fake-url.com/non-existent2.png"
        ));

        mockMvc.perform(delete("/api/my/slots/{slotId}/images", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(deleteForm)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("내 슬롯 이미지 삭제 실패 - 빈 이미지 URL 리스트")
    void deleteMySlotImage_Fail_EmptyImageUrls() throws Exception {
        UserSlotImageUrlForm deleteForm = new UserSlotImageUrlForm();
        deleteForm.setImageUrls(Arrays.asList());

        mockMvc.perform(delete("/api/my/slots/{slotId}/images", slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(deleteForm)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}