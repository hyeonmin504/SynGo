package backend.synGo.chatBot.controller;

import backend.synGo.chatBot.service.Chat;
import backend.synGo.form.ResponseForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class chatBotController {

    private final Chat chatService;

    @Operation(summary = "스케줄러 메타 데이터 요청 API", description = "스케줄러 데이터를 가져옵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공")
    })
    @GetMapping("/my/scheduler")
    public ResponseEntity<ResponseForm<?>> useChatBot() {
        // 여기에 챗봇 사용 로직을 추가해야 합니다.
        // 예시로 단순한 응답을 반환합니다.
        String response = chatService.singleChat("Hello, Chatbot!", 1L);
        return ResponseEntity.ok(ResponseForm.success(response, "챗봇 응답 성공"));
    }
}
