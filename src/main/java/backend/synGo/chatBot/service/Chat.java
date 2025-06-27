package backend.synGo.chatBot.service;

public interface Chat {

    /**
     * 단일 채팅 요청 (히스토리 없이)
     */
    String singleChat(String userInput, Long userId);

    /**
     * 연속 대화 요청 (히스토리 포함)
     */
    String multiChat(String userInput, Long userId);
}