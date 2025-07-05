package backend.synGo.chatBot.controller.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record UploadImageRequest(
        String content,
        @Size(max = 2, message = "이미지는 최대 2개까지 업로드 가능합니다.")
        List<MultipartFile> images
) {
    @Override
    public List<MultipartFile> images() {
        if (Objects.isNull(images) || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images;
    }
}
