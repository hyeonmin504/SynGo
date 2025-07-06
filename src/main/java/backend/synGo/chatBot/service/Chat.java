package backend.synGo.chatBot.service;

public interface Chat {

    /**
     * 단일 일반 채팅 요청 (히스토리 없이)
     */
    String singleChat(String userInput, Long userId);
}
