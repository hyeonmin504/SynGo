package backend.synGo.chatBot.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_model")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // 모델 이름
    private String apiKey; // API 키
    private String modelType;// 모델 이름 (예: gpt-4, claude-3-opus)
    private int maxTokens;  // 최대 토큰 수
    private double temperature; // 창의성 조절

    @OneToMany(mappedBy = "aiModel")
    private List<ChatMessage> chatMessage = new ArrayList<>(); // 연관된 채팅 메시지
}
