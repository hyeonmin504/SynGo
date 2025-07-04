package backend.synGo.chatBot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("openAiService")
public class OpenAiService implements Chat {

    @Override
    public String singleChat(String userInput, Long userId) {
        return null;
    }

    @Override
    public String multiChat(String userInput, Long userId) {
        return null;
    }
}
