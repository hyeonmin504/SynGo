package backend.synGo.chatBot.service;

import backend.synGo.chatBot.controller.ChatBotController;
import backend.synGo.chatBot.domain.ChatHistory;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.repository.ChatHistoryRepository;
import backend.synGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static backend.synGo.chatBot.controller.ChatBotController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final UserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;

    @Async
    @Transactional
    public void saveHistory(ChatRequest chatRequest, String aiResponse) {
        try {
            User user = userRepository.findById(chatRequest.getUserId())
                    .orElseThrow(() -> new NotFoundUserException("User not found with ID: " + chatRequest.getUserId()));
            ChatHistory history = ChatHistory.builder()
                    .userMessage(chatRequest.getMessage())
                    .aiMessage(aiResponse)
                    .user(user)  // 프록시 사용
                    .build();

            chatHistoryRepository.save(history);
            log.info("Chat history saved successfully for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to save chat history", e);
        }
    }
}
