package backend.synGo.chatBot.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatStreamResponse {
    private String content;
    private String error;
    private boolean done;

    public static ChatStreamResponse content(String content) {
        return new ChatStreamResponse(content, null, false);
    }

    public static ChatStreamResponse error(String error) {
        return new ChatStreamResponse(null, error, false);
    }

    public static ChatStreamResponse done() {
        return new ChatStreamResponse(null, null, true);
    }
}