package backend.synGo.chatBot.service;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import backend.synGo.filesystem.ImageValidationService;
import backend.synGo.filesystem.awss3.S3StorageManager;
import backend.synGo.filesystem.util.FileUtil;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class AnthropicStreamChatService implements StreamChat {

    private final AnthropicChatModel chatClient;
    private final ImageValidationService imageValidationService;
    @Value("${aws.s3.image-directory}")
    private String imageDirectory;
    private final S3StorageManager s3StorageManager;

    /**
     * 채팅 요청을 처리하는 메서드
     * @param message 사용자 입력 메시지
     * @param images 업로드된 이미지 파일 배열
     * @return Flux<String> AI 응답 스트림
     */
    @Override
    public Flux<String> streamChatWithAuth(String message, MultipartFile[] images) {
        log.info("Processing chat request for user: {}", message);
        return Mono.fromCallable(() -> createPrompt(message, images))
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업을 별도 스레드에서 처리
                .flatMapMany(prompt -> {
                    log.info("Sending prompt to Anthropic API");
                    return chatStreamApiRequest(prompt);
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
        if (hasImages) {
            return """
            당신은 스케줄 분석 전문 AI 어시스턴트입니다.
            
            주어진 이미지를 분석하여 스케줄 정보를 추출하고, 반드시 다음 JSON 형태로만 응답해주세요.
            
            JSON 형태:
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
            - title, start_date, end_date는 반드시 포함되어야 합니다
            - 이미지에서 필수 정보를 추출할 수 없다면, 사용자에게 해당 정보를 추가로 제공해달라고 일반 텍스트로 요청하세요
            
            응답 규칙:
            - 필수 정보가 모두 있으면: 순수한 JSON만 출력 (설명이나 다른 텍스트 절대 포함 금지)
            - 필수 정보가 부족하면: 일반 텍스트로 추가 정보 요청
            - 날짜는 YYYY-MM-DD HH:mm 형식 준수
            """;
        } else {
            return """
            당신은 스케줄 생성 전문 AI 어시스턴트입니다.
            
            사용자의 요청에 따라 스케줄 정보를 JSON 형태로 생성해주세요.
            
            JSON 형태:
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
            - title, start_date, end_date는 반드시 포함되어야 합니다
            - 사용자가 필수 정보를 제공하지 않았다면, 해당 정보를 추가로 제공해달라고 일반 텍스트로 요청하세요
            
            응답 규칙:
            - 필수 정보(title, start_date, end_date)가 모두 있으면: 순수한 JSON만 출력 (설명이나 인사말 절대 포함 금지)
            - 필수 정보가 부족하면: 일반 텍스트로 추가 정보 요청
            - 날짜는 YYYY-MM-DD HH:mm 형식 준수
            
            중요: JSON 응답 시에는 어떠한 설명도 추가하지 말고 오직 JSON 데이터만 출력하세요.
            """;
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
                // 고유한 파일명 생성
                String originalFileName = image.getOriginalFilename();
                // 이미지 크기 및 확장자 유효성 검사
                imageValidationService.validate(image);
                // S3, Cloudfront 업로드
                String cloudFrontImageUrl = uploadS3AndCloudFront(image, originalFileName);
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
     * @param image 업로드할 이미지 파일
     * @param originalFileName 원본 파일 이름
     * @return CloudFront 이미지 URL
     */
    private String uploadS3AndCloudFront(MultipartFile image, String originalFileName) {
        //S3를 위한 이미지 경로 생성
        String filePath = imageDirectory + "/" + "chat_" + UUID.randomUUID();
        // S3에 이미지 업로드
        String cloudFrontImageUrl = s3StorageManager.upload(image, filePath, originalFileName);
        log.info("Image uploaded to S3: {}", cloudFrontImageUrl);
        return cloudFrontImageUrl;
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

//            // 특수 문자 이스케이프 처리 (JSON 안전성을 위해)
//            return content.replace("\"", "\\\"")
//                    .replace("\n", "\\n")
//                    .replace("\r", "\\r")
//                    .replace("\t", "\\t");
            log.info("Extracted content: {}", content);
            return content;
        } catch (Exception e) {
            log.error("Error extracting content: {}", e.getMessage(), e);
            return ""; // 빈 문자열 반환하여 스트림 중단 방지
        }
    }
}