package backend.synGo.service;

import backend.synGo.domain.slot.UserSlot;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.filesystem.ImageValidationService;
import backend.synGo.filesystem.UploadService;
import backend.synGo.filesystem.awss3.S3StorageManager;
import backend.synGo.filesystem.domain.ImageUrl;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.repository.ImageUrlRepository;
import backend.synGo.repository.UserSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static backend.synGo.controller.my.MySlotImageController.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotImageService {
    private final UserSlotRepository userSlotRepository;
    private final S3StorageManager s3StorageManager;
    private final ImageValidationService imageValidationService;
    private final UploadService uploadService;
    private final ImageUrlRepository imageUrlRepository;
    @Value("${aws.s3.image-directory}")
    private String imageDirectory;

    /**
     * 이미지 업로드 메서드
     * @param slotId 슬롯 ID
     * @param images 업로드할 이미지 파일 배열
     * @param userId 사용자 ID
     * @return 업로드된 이미지의 슬롯 ID 응답
     */
    @Transactional
    public SlotIdResponse uploadImage(Long slotId, MultipartFile[] images, Long userId) {
        UserSlot userSlot = userSlotRepository.findByUserIdAndSlotId(userId, slotId)
                .orElseThrow(() -> new NotFoundContentsException("해당 유저의 슬롯을 찾을 수 없습니다."));
        if (images == null || images.length == 0) {
            throw new NotFoundContentsException("업로드할 이미지가 없습니다.");
        }
        for (MultipartFile image : images) {
            imageValidationService.validate(image); // 이미지 유효성 검사
            String originalFileName = image.getOriginalFilename();
            //S3를 위한 이미지 경로 생성
            String filePath = imageDirectory + "/" + "UserSlot_" + UUID.randomUUID();
            // S3에 이미지 업로드
            String cloudFrontImageUrl = s3StorageManager.upload(image, filePath, originalFileName);
            log.info("Image uploaded to S3: {}", cloudFrontImageUrl);
            //이미지 정보 저장
            uploadService.saveImageAtSlot(cloudFrontImageUrl, userId, image, userSlot);
        }
        return new SlotIdResponse(slotId);
    }

    /**
     * 이미지 삭제 메서드
     * @param slotId 슬롯 ID
     * @param images 삭제할 이미지 URL 리스트
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteImage(Long slotId, List<String> images, Long userId) {
        List<ImageUrl> myImage = imageUrlRepository.findByMyImageUrls(userId, slotId, images);
        if (myImage.isEmpty()) {
            throw new NotFoundContentsException("해당 유저의 슬롯에서 삭제할 이미지가 없습니다.");
        }
        for (ImageUrl imageUrl : myImage) {
            // S3에서 이미지 삭제
            s3StorageManager.delete(imageUrl.getImageUrl());
            log.info("Image deleted from S3: {}", imageUrl.getImageUrl());
            // 이미지 정보 삭제
            uploadService.deleteImage(imageUrl);
        }
    }

    @Transactional(readOnly = true)
    public UserSlotImageUrlForm findMySlotImages(Long slotId, Long userId) {
        if (!userSlotRepository.existUserUserId(userId)) {
            throw new NotFoundContentsException("해당 유저의 슬롯이 아닙니다.");
        }

        List<ImageUrl> images = imageUrlRepository.findByUserIdAndSlotId(userId, slotId);
        if (images.isEmpty()) {
            return new UserSlotImageUrlForm(null);
        }
        return new UserSlotImageUrlForm(images.stream().map(ImageUrl::getImageUrl)
                .collect(Collectors.toList()));
    }
}
