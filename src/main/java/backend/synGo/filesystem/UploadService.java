package backend.synGo.filesystem;

import backend.synGo.domain.user.User;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.filesystem.domain.Image;
import backend.synGo.filesystem.util.FileUtil;
import backend.synGo.repository.ImageRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public void saveImage(String url, Long userId, MultipartFile image) {
        // getReferenceById를 사용하여 프록시만 가져옴
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundUserException("User not found with ID: " + userId));

        imageRepository.save(
                Image.builder()
                        .imageUrl(new backend.synGo.filesystem.domain.image.ImageUrl(url))
                        .imageType(FileUtil.getFileExtension(image).toLowerCase())
                        .imageName(image.getOriginalFilename())
                        .user(user)  // 프록시 사용
                        .build()
        );
    }
}
