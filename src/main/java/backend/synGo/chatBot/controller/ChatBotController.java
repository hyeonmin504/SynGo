package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.service.AnthropicStreamChatService;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import backend.synGo.repository.UserRepository;
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

    @PostMapping
    public ResponseEntity<ResponseForm<?>> generate(@RequestBody String message,
                                                    @ModelAttribute @Valid final MultipartFile[] images,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().body(ResponseForm.success(null, "AI 응답 생성 성공"));
    }

    /**
     * 이미지 업로드 후 채팅 스트리밍
     * @param message
     * @param images
     * @return
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return chatService.streamChatWithAuth(new ChatRequest(message,userDetails.getUserId()), images)
                .doOnNext(response -> log.info("Stream response: {}", response))
                .map(response -> ServerSentEvent.<String>builder()
                        .data("{\"content\": \"" + escapeJson(response) + "\"}")
                        .build())
                .concatWith(
                        Mono.just(ServerSentEvent.<String>builder()
                                .data("{\"done\": true}")
                                .build())
                )
                .doOnComplete(() -> {
                    log.info("Stream completed");
                })
                .doOnCancel(() -> {
                    log.info("Stream cancelled");
                })
                .onErrorResume(error -> {
                    log.error("StreamChat streaming error", error);
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .data("{\"error\":\"" + escapeJson(error.getMessage()) + "\"}")
                                    .build(),
                            ServerSentEvent.<String>builder()
                                    .data("{\"done\": true}")
                                    .build()
                    );
                });
    }

    private String escapeJson(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
        private Long userId;
    }
}