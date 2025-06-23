package backend.synGo.webSocket.messagingStompWebocket;

import lombok.Getter;

@Getter
public class Greeting {
    private String content;

    public Greeting() {
    }

    public Greeting(String content) {
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
