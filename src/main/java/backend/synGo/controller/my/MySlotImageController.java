package backend.synGo.controller.my;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.service.SlotImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/my/slots")
@RequiredArgsConstructor
public class MySlotImageController {

    private final SlotImageService slotImageService;

    @Operation(summary = "My slot 이미지 검색 api", description = "개인 slot의 이미지를 검색하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 이미지 검색 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MySlotController.UserSlotResponseForm.class)
            )),
            @ApiResponse(responseCode = "406", description = "다른 유저의 슬롯 검색으로 인한 에러"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @GetMapping("/{slotId}/images")
    public ResponseEntity<ResponseForm<?>> getMySlotImages(@PathVariable Long slotId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UserSlotImageUrlForm responseForm = slotImageService.findMySlotImages(slotId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(responseForm,"슬롯 이미지 요청 성공"));
        } catch (AccessDeniedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (NotFoundUserException | NotFoundContentsException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "My slot 이미지 등록", description = "개인 slot을 이미지 추가 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 추가 성공"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @PostMapping("/{slotId}/images")
    public ResponseEntity<ResponseForm<?>> updateMySLotImage(
            @PathVariable Long slotId,
            @ModelAttribute MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            SlotIdResponse form = slotImageService.uploadImage(slotId, images, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(form, "이미지 등록 성공"));
        } catch (NotFoundUserException | NotFoundContentsException | DateTimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "My slot 이미지 삭제", description = "개인 slot을 이미지 삭제 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @DeleteMapping("/{slotId}/images")
    public ResponseEntity<ResponseForm<?>> deleteMySLotImage(
            @PathVariable Long slotId,
            @RequestBody UserSlotImageUrlForm imageUrls,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            slotImageService.deleteImage(slotId, imageUrls.getImageUrls(), userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(null,"이미지 삭제 성공"));
        } catch (NotFoundUserException | NotFoundContentsException | DateTimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSlotImageUrlForm {
        private List<String> imageUrls = new ArrayList<>();
    }
}
