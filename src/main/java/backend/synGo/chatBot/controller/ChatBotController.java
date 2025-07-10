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

    @PostMapping
    public ResponseEntity<ResponseForm<?>> Chat(@RequestBody String message,
                                                    @ModelAttribute @Valid final MultipartFile[] images,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().body(ResponseForm.success(null, "AI 응답 생성 성공"));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return validateUser(userDetails)
                .flatMapMany(userId ->
                        chatService.streamChat(new ChatRequest(message, userId), images)
                                .map(ChatStreamResponse::content)
                                .concatWith(Mono.just(ChatStreamResponse.done()))
                )
                .map(response -> ServerSentEvent.<ChatStreamResponse>builder()
                        .data(response)
                        .build())
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

    // 인증 검증 로직 분리
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