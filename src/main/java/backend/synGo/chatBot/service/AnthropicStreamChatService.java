package backend.synGo.chatBot.service;

import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.filesystem.UploadService;
import backend.synGo.filesystem.domain.Image;
import backend.synGo.filesystem.domain.ImageUrl;
import backend.synGo.filesystem.util.FileUtil;
import backend.synGo.repository.ImageRepository;
import backend.synGo.repository.UserRepository;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import backend.synGo.filesystem.ImageValidationService;
import backend.synGo.filesystem.awss3.S3StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static backend.synGo.chatBot.controller.ChatBotController.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class AnthropicStreamChatService implements StreamChat {

    private final AnthropicChatModel chatClient;
    private final ImageValidationService imageValidationService;
    private final ChatHistoryService chatHistoryService;
    private final S3StorageManager s3StorageManager;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final UploadService uploadService;

    @Value("${aws.s3.image-directory}")
    private String imageDirectory;

    /**
     * 채팅 요청을 처리하는 메서드
     * @param chatRequest 사용자 입력 메시지
     * @param images 업로드된 이미지 파일 배열
     * @return Flux<String> AI 응답 스트림
     */
    @Override
    public Flux<String> streamChat(ChatRequest chatRequest, MultipartFile[] images) {
        log.info("Processing chat request for user: {}", chatRequest.getMessage());
        // 전체 응답을 저장할 StringBuilder
        StringBuilder fullResponse = new StringBuilder();
        // 각 청크를 StringBuilder에 추가
        return Mono.fromCallable(() -> createPrompt(chatRequest, images)) // 프롬프트 생성
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업을 별도 스레드에서 처리
                .flatMapMany(prompt -> {
                    log.info("Sending prompt to Anthropic API");
                    return chatStreamApiRequest(prompt);
                })
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // 스트림 완료 시 전체 메시지 저장
                    String completeResponse = fullResponse.toString();
                    // 히스토리 저장
                    chatHistoryService.saveHistory(
                            chatRequest,
                            completeResponse
                    );
//                    if (images != null && images.length > 0) {
//                        uploadS3AndCloudFront(images, chatRequest.getUserId());
//                    }
                })
                .onErrorResume(error -> {
                    log.error("에러 발생: {}", error.getMessage(), error);
                    return Flux.just("⚠️ 요청 중 오류가 발생했습니다");
                });
    }

    /**
     * Anthropic API에 프롬프트를 보내고 스트리밍 응답을 처리하는 메서드
     * @param prompt 생성된 프롬프트 객체
     * @return Flux<String> AI 응답 스트림
     */
    private Flux<String> chatStreamApiRequest(Prompt prompt) {
        return chatClient.stream(prompt)
                .doOnNext(response -> log.debug("Received response chunk"))
                .map(this::extractContent)
                .filter(content -> content != null && !content.isEmpty())
                .onErrorResume(error -> {
                    log.error("Anthropic API error: {}", error.getMessage(), error);
                    return Flux.just("AI 서비스 오류가 발생했습니다: " + error.getMessage());
                });
    }

    /**
     * 프롬프트 생성 메서드
     * @param chatRequest 사용자 입력 메시지
     * @param images 업로드된 이미지 파일 배열
     * @return 생성된 프롬프트 객체
     */
    private Prompt createPrompt(ChatRequest chatRequest, MultipartFile[] images) {
        try {
            List<Message> messages = new ArrayList<>();
            // SystemMessage 먼저 추가
            String systemPrompt = createSystemPrompt(images != null && images.length > 0);
            messages.add(new SystemMessage(systemPrompt));
            if (images != null && images.length > 0) {
                // 이미지 처리 로직
                List<Media> mediaList = getMedia(images);
                // UserMessage 생성 - 텍스트와 이미지 모두 포함
                UserMessage userMessage = new UserMessage(chatRequest.getMessage(), mediaList);
                messages.add(userMessage);
            } else {
                // 텍스트만 있는 경우
                UserMessage userMessage = new UserMessage(chatRequest.getMessage());
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
                    스케줄 생성 AI입니다.
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
                          "important": "VERY_LOW | LOW | MEDIUM | HIGH | VERY_HIGH"
                        }
                      ]
                    }
                            
                    필수 요구사항:
                    - calculateDateTime,getCurrentDateTime Tool을 활용
                    - 12시간제(오후 2시) → 24시간제(14:00)
                    - 상대적 날짜/시간은 현재 시각(%s)을 기준으로 계산하세요
                            
                    응답 규칙:
                    - 필수 정보가 모두 있으면: 순수한 JSON만 출력 (설명이나 다른 텍스트 절대 포함 금지)
                    - 부족한 정보는 추측해서 응답
                    
                    중요: 어떠한 설명도 추가하지 말고 오직 JSON 데이터만 출력하세요.
                    """, currentDate);
        } else {
            return String.format("""
                    스케줄 생성 AI입니다.
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
                          "important": "VERY_LOW | LOW | MEDIUM | HIGH | VERY_HIGH"
                        },
                        ...
                      ]
                    }
                    
                    필수 요구사항:
                    - calculateDateTime,getCurrentDateTime Tool을 활용
                    - 12시간제(오후 2시) → 24시간제(14:00)
                    - 상대적 날짜/시간은 현재 시각(%s)을 기준으로 계산하세요
                    
                    응답 규칙:
                    - 필수 정보가 모두 있으면: 순수한 JSON만 출력 (설명이나 다른 텍스트 절대 포함 금지)
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

    /**
     * S3에 이미지를 업로드하고 CloudFront URL을 반환
     * @param images 업로드할 이미지 파일
     * @param userId 저장할 유저
     * @return CloudFront 이미지 URL
     */
    private void uploadS3AndCloudFront(MultipartFile[] images, Long userId) {
        for (MultipartFile image : images) {
            String originalFileName = image.getOriginalFilename();
            //S3를 위한 이미지 경로 생성
            String filePath = imageDirectory + "/" + "chat_" + UUID.randomUUID();
            // S3에 이미지 업로드
            String cloudFrontImageUrl = s3StorageManager.upload(image, filePath, originalFileName);
            log.info("Image uploaded to S3: {}", cloudFrontImageUrl);
            //이미지 정보 저장
            uploadService.saveImage(cloudFrontImageUrl, userId, image);
        }
    }

    /**
     * ChatResponse에서 안전하게 내용 추출
     */
    private String extractContent(ChatResponse response) {
        try {
            if (response == null) {
                log.warn("ChatResponse is null");
                return "";
            }

            if (response.getResult() == null) {
                log.warn("ChatResponse.getResult() is null");
                return "";
            }

            if (response.getResult().getOutput() == null) {
                log.warn("ChatResponse.getResult().getOutput() is null");
                return "";
            }

            String content = response.getResult().getOutput().getContent();
            if (content == null) {
                log.warn("Content is null");
                return "";
            }

            log.info("Extracted content: {}", content);
            return content;
        } catch (Exception e) {
            log.error("Error extracting content: {}", e.getMessage(), e);
            return ""; // 빈 문자열 반환하여 스트림 중단 방지
        }
    }
}