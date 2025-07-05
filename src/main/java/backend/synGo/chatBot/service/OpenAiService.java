package backend.synGo.chatBot.service;

import backend.synGo.chatBot.controller.ChatBotController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("openAiService")
public class OpenAiService implements Chat {

    @Override
    public String singleChat(String userInput, Long userId) {
        return null;
    }

    @Override
    public Flux<String> streamChatWithAuth(String request, String token) {
        return null;
    }

    @Override
    public Flux<String> streamChatWithAuth(ChatBotController.ChatRequest request, MultipartFile[] images) {
        return null;
    }


}
