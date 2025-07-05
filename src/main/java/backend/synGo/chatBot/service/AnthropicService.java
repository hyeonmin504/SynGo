package backend.synGo.chatBot.service;

import backend.synGo.auth.form.TokenType;
import backend.synGo.config.jwt.JwtProvider;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static backend.synGo.chatBot.controller.ChatBotController.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class AnthropicService implements Chat {

    private final JwtProvider jwtProvider;
    private final AnthropicChatModel chatClient;
    private final ImageValidationService imageValidationService;
    @Value("${aws.s3.image-directory}")
    private String imageDirectory;
    private final S3StorageManager s3StorageManager;

    @Override
    public String singleChat(String userInput, Long userId) {
        return null;
    }

    @Override
    public Flux<String> streamChatWithAuth(String request, String token) {
        return Mono.fromCallable(() -> jwtProvider.validateToken(token, TokenType.TOKEN))
                .flatMapMany(userInfo -> {
                    Prompt prompt = new Prompt(request);
                    return chatClient.stream(prompt)
                            .map(this::extractContent)
                            .filter(content -> content != null && !content.isEmpty());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> streamChatWithAuth(ChatRequest request, MultipartFile[] images) {
        log.info("Processing chat request for user: {}", request.getUserId());

        // 개발 모드에서는 인증 체크 생략
        log.info("Development mode - skipping authentication");

        return processChat(request, images)
                .onErrorResume(error -> {
                    log.error("에러 발생: {}", error.getMessage(), error);
                    return Flux.just("⚠️ 오류 발생: " + error.getMessage());
                })
                .doOnComplete(() -> {
                    log.info("ChatService stream completed for user: {}", request.getUserId());
                });
    }

    private Flux<String> processChat(ChatRequest request, MultipartFile[] images) {
        return Mono.fromCallable(() -> createPrompt(request, images))
                .subscribeOn(Schedulers.boundedElastic()) // 블로킹 작업을 별도 스레드에서 처리
                .flatMapMany(prompt -> {
                    log.info("Sending prompt to Anthropic API");
                    return chatClient.stream(prompt)
                            .doOnNext(response -> log.debug("Received response chunk"))
                            .map(this::extractContent)
                            .filter(content -> content != null && !content.isEmpty())
                            .onErrorResume(error -> {
                                log.error("Anthropic API error: {}", error.getMessage(), error);
                                return Flux.just("AI 서비스 오류가 발생했습니다: " + error.getMessage());
                            });
                })
                .doOnError(error -> log.error("Process chat error: {}", error.getMessage(), error));
    }

    private Prompt createPrompt(ChatRequest request, MultipartFile[] images) {
        try {
            List<Message> messages = new ArrayList<>();

            if (images != null && images.length > 0) {
                // 이미지 처리 로직
                List<Media> mediaList = new ArrayList<>();
                for (MultipartFile image : images) {
                    try {
                        // 고유한 파일명 생성
                        String originalFileName = image.getOriginalFilename();
                        imageValidationService.validate(image); // 이미지 유효성 검사

                        String filePath = imageDirectory + "/" + "chat_" + UUID.randomUUID().toString();

                        // S3에 이미지 업로드
                        String cloudFrontImageUrl = s3StorageManager.upload(image, filePath, originalFileName);
                        log.info("Image uploaded to S3: {}", cloudFrontImageUrl);

                        // URL을 통해 Media 객체 생성
                        String mimeType = image.getContentType();
                        if (mimeType == null) {
                            mimeType = "image/jpeg"; // 기본 MIME 타입
                        }

                        URL url = new URL(cloudFrontImageUrl);
                        Media media = new Media(MimeTypeUtils.parseMimeType(mimeType), url);
                        mediaList.add(media);

                    } catch (Exception e) {
                        log.error("Error processing image: {}", e.getMessage(), e);
                        throw new RuntimeException("이미지 처리 중 오류가 발생했습니다: " + e.getMessage());
                    }
                }

                // UserMessage 생성 - 텍스트와 이미지 모두 포함
                UserMessage userMessage = new UserMessage(request.getMessage(), mediaList);
                messages.add(userMessage);
            } else {
                // 텍스트만 있는 경우
                UserMessage userMessage = new UserMessage(request.getMessage());
                messages.add(userMessage);
            }

            return new Prompt(messages);

        } catch (Exception e) {
            log.error("Error creating prompt: {}", e.getMessage(), e);
            throw new RuntimeException("프롬프트 생성 중 오류가 발생했습니다: " + e.getMessage());
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