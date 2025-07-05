package backend.synGo.chatBot.service;

import backend.synGo.chatBot.controller.ChatBotController;
import backend.synGo.filesystem.awss3.S3StorageManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.ai.model.Media;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ChatService {

    @Autowired
    private AnthropicChatModel chatClient;
    @Autowired
    private S3StorageManager s3StorageManager;
    @Value("${aws.s3.image-directory}")
    private String imageDirectory;

    public Flux<String> streamChatWithAuth(ChatBotController.ChatRequest request, MultipartFile[] images) {
        log.info("Processing chat request for user: {}", request.getUserId());

        return Mono.fromCallable(() -> {
                    // 프롬프트 생성
                    List<Message> messages = new ArrayList<>();

                    if (images != null && images.length > 0) {
                        // 이미지 처리 로직
                        List<Media> mediaList = new ArrayList<>();
                        for (MultipartFile image : images) {
                            try {
                                // 고유한 파일명 생성 (중복 방지)
                                String originalFileName = image.getOriginalFilename();
                                String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                                String uniqueFileName = "chat_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + fileExtension;
                                String filePath = imageDirectory + "/" + uniqueFileName;

                                // S3에 이미지 업로드
                                String imageUrl = s3StorageManager.upload(image, filePath, originalFileName);
                                log.info("Image uploaded to S3: {}", imageUrl);

                                // URL을 통해 Media 객체 생성 (Anthropic은 URL 방식을 지원)
                                String mimeType = image.getContentType();
                                URL url = new URL(imageUrl);
                                Media media = new Media(MimeTypeUtils.parseMimeType(mimeType), url);
                                mediaList.add(media);

                            } catch (Exception e) {
                                log.error("Error processing image", e);
                                throw new RuntimeException("이미지 처리 중 오류가 발생했습니다", e);
                            }
                        }

                        // UserMessage 생성 방식 수정
                        UserMessage userMessage = new UserMessage(request.getMessage(), mediaList);
                        messages.add(userMessage);
                    } else {
                        // 텍스트만 있는 경우
                        UserMessage userMessage = new UserMessage(request.getMessage());
                        messages.add(userMessage);
                    }

                    return new Prompt(messages);
                })
                .flatMapMany(prompt -> {
                    return chatClient.stream(prompt)
                            .map(response -> {
                                String content = response.getResult().getOutput().getContent();
                                // JSON 형태로 반환하여 프론트엔드에서 파싱하기 쉽게 함
                                return String.format("{\"result\":{\"output\":{\"content\":\"%s\"}}}",
                                        content.replace("\"", "\\\"").replace("\n", "\\n"));
                            })
                            .onErrorResume(error -> {
                                log.error("Anthropic API error: {}", error.getMessage());
                                return Flux.just("{\"error\":\"AI 서비스 오류가 발생했습니다\"}");
                            });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .contextWrite((ContextView) ReactiveSecurityContextHolder.getContext().map(securityContext ->
                        Context.of(SecurityContext.class, securityContext)
                ));
    }
}