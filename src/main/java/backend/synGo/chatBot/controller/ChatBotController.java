package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.controller.dto.UploadImageRequest;
import backend.synGo.chatBot.service.AnthropicService;
import backend.synGo.form.ResponseForm;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/my/chatbot")
public class ChatBotController {

    private final AnthropicChatModel chatModel;
    private final AnthropicService chatService;

    @PostMapping
    public ResponseEntity<ResponseForm<?>> generate(@RequestBody String message,
                                                    @ModelAttribute @Valid final UploadImageRequest uploadImageRequest,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Generating response for user: {}", userDetails.getUsername());
        String response = chatModel.call(message);
        return ResponseEntity.ok().body(ResponseForm.success(Map.of("generation", response), "AI 응답 생성 성공"));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) MultipartFile[] images) {

        log.info("Chat stream request received - message: {}", message);

        // 기본 사용자 ID 설정 (개발용)
        String userId = "dev-user";

        // 채팅 요청 객체 생성
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(message != null ? message : "Hello");
        chatRequest.setUserId(userId);

        // AnthropicService의 streamChatWithAuth 대신 직접 처리하거나
        // 인증 없는 메서드 사용
        return chatService.streamChatWithAuth(chatRequest, images)
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
                    log.error("Chat streaming error", error);
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Access Denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
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
        private String userId;
    }
}