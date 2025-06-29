package backend.synGo.chatBot.controller.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UploadImageRequest(
        List<MultipartFile> image
) {}
