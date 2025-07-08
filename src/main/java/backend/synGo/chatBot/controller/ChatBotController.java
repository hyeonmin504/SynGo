package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.dto.ChatStreamResponse;
import backend.synGo.chatBot.service.AnthropicStreamChatService;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import backend.synGo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/my/chatbot")
public class ChatBotController {

    private final AnthropicStreamChatService chatService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ResponseForm<?>> generate(@RequestBody String message,
                                                    @ModelAttribute @Valid final MultipartFile[] images,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().body(ResponseForm.success(null, "AI 응답 생성 성공"));
    }

//    /**
//     * 이미지 업로드 후 채팅 스트리밍
//     *
//     * @param message
//     * @param images
//     * @return
//     */
//    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ServerSentEvent<String>> streamChat(
//            @RequestParam String message,
//            @RequestParam(required = false) MultipartFile[] images,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        // 1. 인증 검증을 별도 메서드로 분리
//        return validateUser(userDetails)
//                .flatMapMany(userId -> processStreamChat(message, images, userId))
//                .doOnNext(event -> log.debug("Stream event: {}", event.data()))
//                .doOnComplete(() -> log.info("Stream completed for user: {}", userDetails.getUserId()))
//                .doOnCancel(() -> log.info("Stream cancelled for user: {}", userDetails.getUserId()))
//                .onErrorResume(this::handleStreamError);
//    }
//

//
//    // 3. 메인 스트림 처리 로직 분리
//    private Flux<ServerSentEvent<String>> processStreamChat(String message, MultipartFile[] images, Long userId) {
//        return chatService.streamChatWithAuth(new ChatRequest(message, userId), images)
//                .map(this::createContentEvent)
//                .concatWith(Mono.just(createDoneEvent()));
//    }
//
//    // 4. SSE 이벤트 생성 메서드들
//    private ServerSentEvent<String> createContentEvent(String content) {
//        return ServerSentEvent.<String>builder()
//                .data(String.format("{\"content\": \"%s\"}", escapeJson(content)))
//                .build();
//    }
//
//    private ServerSentEvent<String> createDoneEvent() {
//        return ServerSentEvent.<String>builder()
//                .data("{\"done\": true}")
//                .build();
//    }
//
//    private ServerSentEvent<String> createErrorEvent(String errorMessage) {
//        return ServerSentEvent.<String>builder()
//                .data(String.format("{\"error\": \"%s\"}", escapeJson(errorMessage)))
//                .build();
//    }
//
//    // 5. 에러 처리 로직 분리
//    private Flux<ServerSentEvent<String>> handleStreamError(Throwable error) {
//        log.error("StreamChat error occurred", error);
//        return Flux.just(
//                createErrorEvent(error.getMessage()),
//                createDoneEvent()
//        );
//    }
//
//    // 6. JSON 이스케이프 개선 (Jackson 사용 권장)
//    private String escapeJson(String input) {
//        if (input == null) return "";
//        try {
//            return objectMapper.writeValueAsString(input).replaceAll("^\"|\"$", "");
//        } catch (Exception e) {
//            log.warn("JSON escape failed, using manual escape", e);
//            return input.replace("\\", "\\\\")
//                    .replace("\"", "\\\"")
//                    .replace("\n", "\\n")
//                    .replace("\r", "\\r")
//                    .replace("\t", "\\t");
//        }
//    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChatV2(
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return validateUser(userDetails)
                .flatMapMany(userId ->
                        chatService.streamChatWithAuth(new ChatRequest(message, userId), images)
                                .map(ChatStreamResponse::content)
                                .concatWith(Mono.just(ChatStreamResponse.done()))
                )
                .map(response -> ServerSentEvent.<ChatStreamResponse>builder()
                        .data(response)
                        .build())
                .doOnNext(event -> log.debug("Stream event: {}", event.data()))
                .doOnComplete(() -> log.info("Stream completed"))
                .doOnCancel(() -> log.info("Stream cancelled"))
                .onErrorResume(error -> {
                    log.error("StreamChat error", error);
                    return Flux.just(
                            ServerSentEvent.<ChatStreamResponse>builder()
                                    .data(ChatStreamResponse.error(error.getMessage()))
                                    .build(),
                            ServerSentEvent.<ChatStreamResponse>builder()
                                    .data(ChatStreamResponse.done())
                                    .build()
                    );
                });
    }

    // 2. 인증 검증 로직 분리
    private Mono<Long> validateUser(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            return Mono.error(new NotFoundUserException("로그인 후 이용해주세요"));
        }
        return Mono.just(userDetails.getUserId());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
        private Long userId;
    }
}