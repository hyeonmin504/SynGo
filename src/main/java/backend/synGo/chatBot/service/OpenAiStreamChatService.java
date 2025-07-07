package backend.synGo.chatBot.service;

import backend.synGo.chatBot.controller.ChatBotController;
import backend.synGo.filesystem.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.net.URL;

import static backend.synGo.chatBot.controller.ChatBotController.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Qualifier("openAiService")
public class OpenAiStreamChatService implements StreamChat {

    @Override
    public Flux<String> streamChatWithAuth(ChatRequest message, MultipartFile[] images) {
        return null;
    }


    /**
     * CloudFront URL을 사용하여 Media 객체 생성
     * @param image 업로드된 이미지 파일
     * @param cloudFrontImageUrl CloudFront에 업로드된 이미지 URL
     * @return Media 객체
     * @throws MalformedURLException URL 형식이 잘못된 경우 예외 발생
     */
    private static Media mediaForUrl(MultipartFile image, String cloudFrontImageUrl) throws MalformedURLException {
        // 파일 확장자 추출
        String fileType = FileUtil.getFileExtension(image).toLowerCase();
        String mimeType = FileUtil.getMimeTypeFromExtension(fileType);// MIME 타입 추출

        URL url = new URL(cloudFrontImageUrl);
        return new Media(MimeTypeUtils.parseMimeType(mimeType), url);
    }
}
