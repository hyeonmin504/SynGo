package backend.synGo.chatBot.service;

import backend.synGo.domain.user.User;
import backend.synGo.exception.JsonParsingException;
import backend.synGo.filesystem.ImageValidationService;
import backend.synGo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static backend.synGo.chatBot.controller.ChatBotController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicChatService implements Chat {

    private final AnthropicChatModel chatClient;
    private final UserService userService;
    private final ImageValidationService imageValidationService;
    private final ChatHistoryService chatHistoryService;

    @Override
    public String singleChat(String message, MultipartFile[] images) {
        Prompt prompt = createPrompt(message, images);
        return chatApiRequest(prompt);
    }

    public ChatResponse chat(String message, Long userId, MultipartFile[] images) {
        // 사용자 검증
        User user = userService.findUserById(userId);

        // AI 응답 생성
        String aiChat = singleChat(message, images);

        // 히스토리 저장
        chatHistoryService.saveHistory(message, userId, aiChat);

        // JSON 파싱 및 응답 생성
        ChatResponse chatResponse = createChatResponse(aiChat);

        log.info("AI 응답 생성 완료");
        return chatResponse;
    }

    /**
     * AI 응답을 JSON으로 파싱하여 ChatResponse 객체로 변환
     * @param aiResponse AI 응답 문자열
     * @return ChatResponse 객체
     */
    private ChatResponse createChatResponse(String aiResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // JSON 추출 로직
            String jsonString = extractJsonFromResponse(aiResponse);

            return objectMapper.readValue(jsonString, ChatResponse.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new JsonParsingException("JSON 파싱 실패:");
        }
    }

    /**
     * AI 응답에서 JSON 부분을 추출하는 메서드
     * @param response AI 응답 문자열
     * @return 추출된 JSON 문자열
     */
    private String extractJsonFromResponse(String response) {
        // 중괄호로 시작하고 끝나는 JSON 패턴 찾기
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }

        // JSON을 찾을 수 없는 경우
        throw new JsonParsingException("응답에서 JSON을 찾을 수 없습니다.");
    }

    /**
     * 프롬프트 생성 메서드
     * @param message 사용자 입력 메시지
     * @param images 업로드된 이미지 파일 배열
     * @return 생성된 프롬프트 객체
     */
    private Prompt createPrompt(String message, MultipartFile[] images) {
        try {
            List<Message> messages = new ArrayList<>();
            // SystemMessage 먼저 추가
            String systemPrompt = createSystemPrompt(images != null && images.length > 0);
            messages.add(new SystemMessage(systemPrompt));
            if (images != null && images.length > 0) {
                // 이미지 처리 로직
                List<Media> mediaList = getMedia(images);
                // UserMessage 생성 - 텍스트와 이미지 모두 포함
                UserMessage userMessage = new UserMessage(message, mediaList);
                messages.add(userMessage);
            } else {
                // 텍스트만 있는 경우
                UserMessage userMessage = new UserMessage(message);
                messages.add(userMessage);
            }
            return new Prompt(messages);
        } catch (Exception e) {
            log.error("Error creating prompt: {}", e.getMessage(), e);
            throw new RuntimeException("프롬프트 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 시스템 프롬프트 생성 메서드
     * @param hasImages 이미지가 있는지 여부
     * @return 생성된 시스템 프롬프트 문자열
     */
    private String createSystemPrompt(boolean hasImages) {
        LocalDateTime now = LocalDateTime.now();
        String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        if (hasImages) {
            return String.format("""
                    스케줄 생성 AI입니다
                    텍스트와 이미지를 참고하여 일정을 다음 JSON 형식으로  반환하세요
                    
                    JSON 형식:
                    {
                      "schedules": [
                        {
                          "title": "일정 제목",
                          "content": "일정 내용 상세 설명",
                          "start_date": "YYYY-MM-DD HH:mm",
                          "end_date": "YYYY-MM-DD HH:mm",
                          "place": "장소",
                          "important": "매우 낮음 | 낮음 | 보통 | 높음 | 매우 높음"
                        }
                      ]
                    }
                            
                    필수 요구사항:
                    - calculateDateTime,getCurrentDateTime Tool을 활용
                    - 12시간제(오후 2시) → 24시간제(14:00)
                    - 상대적 날짜/시간은 현재 시각(%s)을 기준으로 계산하세요
                            
                    응답 규칙:
                    - JSON에 정보를 모두 입력하고 순수한 JSON만 출력 (설명이나 다른 텍스트 절대 포함 금지)
                    - 부족한 정보는 추측해서 응답
                    
                    중요: 어떠한 설명도 추가하지 말고 오직 JSON 데이터만 출력하세요.
                    """, currentDate);
        } else {
            return String.format("""
                    스케줄 생성 AI입니다
                    텍스트를 참고하여 일정을 다음 JSON 형식만 반환하세요
                            
                    JSON 형식:
                    {
                      "schedules": [
                        {
                          "title": "일정 제목",
                          "content": "일정 내용 상세 설명",
                          "start_date": "YYYY-MM-DD HH:mm",
                          "end_date": "YYYY-MM-DD HH:mm",
                          "place": "장소",
                          "important": "매우 낮음 | 낮음 | 보통 | 높음 | 매우 높음"
                        },
                        ...
                      ]
                    }
                    
                    필수 요구사항:
                    - calculateDateTime,getCurrentDateTime Tool을 활용
                    - 12시간제(오후 2시) → 24시간제(14:00)
                    - 상대적 날짜/시간은 현재 시각(%s)을 기준으로 계산하세요
                    
                    응답 규칙:
                    - JSON에 정보를 모두 입력하고 순수한 JSON만 출력 (설명이나 다른 텍스트 절대 포함 금지)
                    - 부족한 정보는 추측해서 응답
                    
                    중요: 어떠한 설명도 추가하지 말고 오직 JSON 데이터만 출력하세요.
                    """,currentDate);
        }
    }

    /**
     * 업로드된 이미지 파일을 처리하여 Media 객체 리스트로 변환
     * @param images 업로드된 이미지 파일 배열
     * @return Media 객체 리스트
     */
    private List<Media> getMedia(MultipartFile[] images) {
        List<Media> mediaList = new ArrayList<>();
        for (MultipartFile image : images) {
            try {
                // 이미지 크기 및 확장자 유효성 검사
                imageValidationService.validate(image);
                // ✅ Spring AI의 올바른 방식
                byte[] imageBytes = image.getBytes();
                String mimeType = image.getContentType();

                // Resource 객체로 감싸서 전달
                Resource imageResource = new ByteArrayResource(imageBytes);
                Media media = new Media(MimeTypeUtils.parseMimeType(mimeType), imageResource);
                mediaList.add(media);
            } catch (Exception e) {
                log.error("Error processing image: {}", e.getMessage(), e);
                throw new RuntimeException("이미지 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        return mediaList;
    }

    private String chatApiRequest(Prompt prompt) {
        return chatClient.call(prompt)
                .getResult()
                .getOutput()
                .getContent();
    }
}