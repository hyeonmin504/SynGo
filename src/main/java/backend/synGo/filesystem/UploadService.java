package backend.synGo.filesystem;

import backend.synGo.domain.slot.UserSlot;
import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.filesystem.domain.Image;
import backend.synGo.filesystem.domain.ImageUrl;
import backend.synGo.filesystem.util.FileUtil;
import backend.synGo.repository.ImageRepository;
import backend.synGo.repository.ImageUrlRepository;
import backend.synGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final ImageRepository imageRepository;
    private final ImageUrlRepository imageUrlRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveImage(String url, Long userId, MultipartFile image) {
        // getReferenceById를 사용하여 프록시만 가져옴
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundUserException("User not found with ID: " + userId));

        imageRepository.save(
                Image.builder()
                        .imageUrl(new backend.synGo.filesystem.domain.ImageUrl(url, user))
                        .imageType(FileUtil.getFileExtension(image).toLowerCase())
                        .imageName(image.getOriginalFilename())
                        .build()
        );
    }

    @Transactional
    public void saveImageAtSlot(String url, Long userId, MultipartFile image, UserSlot userSlot) {
        // getReferenceById를 사용하여 프록시만 가져옴
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundUserException("User not found with ID: " + userId));

        imageRepository.save(
                Image.builder()
                        .imageUrl(new backend.synGo.filesystem.domain.ImageUrl(url, user))
                        .imageType(FileUtil.getFileExtension(image).toLowerCase())
                        .imageName(image.getOriginalFilename())
                        .userSlot(userSlot)
                        .build()
        );
    }


    public void deleteImage(ImageUrl imageUrl) {
        // 이미지 URL이 null이 아니고, 비어있지 않은 경우에만 삭제
        if (imageUrl != null && imageUrl.getImageUrl() != null && !imageUrl.getImageUrl().isEmpty()) {
            imageUrlRepository.delete(imageUrl);
        } else {
            log.warn("Attempted to delete an image with a null or empty URL.");
        }
    }
}
