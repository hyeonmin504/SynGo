package backend.synGo.repository;

import backend.synGo.chatBot.domain.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory,Long> {
}
