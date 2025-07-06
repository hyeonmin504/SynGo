package backend.synGo.chatBot.service;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

public interface StreamChat {

    /**
     * 이미지 포함 연속 대화 요청 (히스토리 포함)
     * @param message
     * @param images
     * @return
     */
    public Flux<String> streamChatWithAuth(String message, MultipartFile[] images);
}