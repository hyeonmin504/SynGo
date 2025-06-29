package backend.synGo.chatBot.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.chatBot.service.Chat;
import backend.synGo.form.ResponseForm;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatBotController {

    private final AnthropicChatModel chatModel;

    @PostMapping("/ai/generate")
    public ResponseEntity<ResponseForm<?>> generate(@RequestBody String message,
                                                    ,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok().body(ResponseForm.success(Map.of("generation", this.chatModel.call(message)), "AI 응답 생성 성공"));
    }

    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }
}
