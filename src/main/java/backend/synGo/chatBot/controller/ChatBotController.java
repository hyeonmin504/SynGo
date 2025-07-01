package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.controller.dto.UploadImageRequest;
import backend.synGo.chatBot.service.Chat;
import backend.synGo.form.ResponseForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my/chatbot")
public class ChatBotController {

    private final AnthropicChatModel chatModel;

    private final Chat chatService;

    @PostMapping
    public ResponseEntity<ResponseForm<?>> generate(@RequestBody String message,
                                                    @ModelAttribute @Valid final UploadImageRequest uploadImageRequest,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok().body(ResponseForm.success(Map.of("generation", this.chatModel.call(message)), "AI 응답 생성 성공"));
    }

    @GetMapping("/stream")
    public Flux<ChatResponse> generateStream(@RequestBody String message,
                                             @ModelAttribute @Valid final UploadImageRequest uploadImageRequest,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }
}
