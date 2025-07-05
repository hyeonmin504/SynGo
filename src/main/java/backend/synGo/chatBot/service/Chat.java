package backend.synGo.chatBot.service;

import backend.synGo.chatBot.controller.ChatBotController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import static backend.synGo.chatBot.controller.ChatBotController.*;

public interface Chat {

    /**
     * 단일 채팅 요청 (히스토리 없이)
     */
    String singleChat(String userInput, Long userId);

    /**
     * 연속 대화 요청 (히스토리 포함)
     */
    public Flux<String> streamChatWithAuth(String request, String token);

    /**
     * 이미지 포함 연속 대화 요청 (히스토리 포함)
     * @param request
     * @param images
     * @return
     */
    public Flux<String> streamChatWithAuth(ChatRequest request, MultipartFile[] images);
}