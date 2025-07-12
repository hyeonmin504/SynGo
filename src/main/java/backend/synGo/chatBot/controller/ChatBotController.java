package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.dto.ChatStreamResponse;
import backend.synGo.chatBot.service.AnthropicChatService;
import backend.synGo.chatBot.service.AnthropicStreamChatService;
import backend.synGo.exception.JsonParsingException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/my/chatbot")
public class ChatBotController {

    private final AnthropicStreamChatService StreamChatService;
    private final AnthropicChatService chatService;

    @Operation(summary = "챗봇 Ai api", description = "이미지와 채팅을 분석 후 springAi(Ai + Tools)를 통해 스케줄 데이터 폼으로 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ai 응답 성공"),
            @ApiResponse(responseCode = "404", description = "유저 정보 없음"),
            @ApiResponse(responseCode = "400", description = "Ai 응답 파싱 실패")
    })
    @PostMapping
    public ResponseEntity<ResponseForm<?>> Chat(
            @RequestParam String message,
            @RequestParam(required = false) final MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            ChatResponse chatResponse = chatService.chat(message, userDetails.getUserId(), images);
            return ResponseEntity.ok().body(ResponseForm.success(chatResponse, "AI 응답 생성 성공"));
        } catch (NotFoundUserException e) {
            log.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        } catch (JsonParsingException e) {
            log.error("JSON parsing failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseForm.badResponse(null, e.getMessage()));
        } catch (TransientAiException e) {
            log.error("Transient AI error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ResponseForm.requestTimeOutResponse(null, "AI 서비스 일시적 오류"));
        }
    }

    @Operation(summary = "챗봇 Stream Ai api", description = "채팅을 분석 후 springAi(Ai + MCP + Tools)를 사용한 내 스케줄 정보 응답")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SSE 연결 성공",
            content = @Content(
                mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                examples = {
                    @ExampleObject(
                            name = "성공",
                            description = "스케줄 요약 정보 스트리밍",
                            value = "data: {\"content\":\"오늘 오후 3시 회의가 있습니다.\"}\ndata: {\"done\":true}"
                    ),
                    @ExampleObject(
                            name = "실패",
                            description = "에러 메시지 스트리밍",
                            value = "data: {\"error\":\"사용자 정보를 찾을 수 없습니다.\"}\ndata: {\"done\":true}"
                    )
                }
            )
        )
    })
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) final MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return validateUser(userDetails)
                .flatMapMany(userId ->
                        StreamChatService.streamChat(new ChatRequest(message, userId), images)
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
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
        private Long userId;
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class ChatResponse {
        private List<Schedule> schedules;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Schedule {
            private String title;
            private String content;

            @JsonProperty("start_date")
            private String startDate;

            @JsonProperty("end_date")
            private String endDate;

            private String place;
            private String important;
        }
    }
}