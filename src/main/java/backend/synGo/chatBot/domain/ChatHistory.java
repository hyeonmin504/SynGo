package backend.synGo.chatBot.domain;

import backend.synGo.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    private String userMessage;
    @Column(length = 10000)
    private String aiMessage;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public ChatHistory(String userMessage, String aiMessage, User user) {
        this.userMessage = userMessage;
        this.aiMessage = aiMessage;
        addUser(user);
    }

    public void addUser(User user) {
        this.user = user;
        user.getChatHistory().add(this);
    }
}
