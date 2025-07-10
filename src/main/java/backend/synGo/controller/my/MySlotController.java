package backend.synGo.controller.my;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.service.SlotImageService;
import backend.synGo.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/my/slots")
@RequiredArgsConstructor
public class MySlotController {

    private final SlotService slotService;

    @Operation(summary = "slot 생성 api", description = "개인 slot을 생성하고 date에 맵핑하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 생성 성공"),
            @ApiResponse(responseCode = "200", description = "날자 오류로 인한 에러"),
            @ApiResponse(responseCode = "404", description = "유저 정보 없음")
    })
    @PostMapping
    public ResponseEntity<ResponseForm<?>> createMySlot(@Validated @RequestBody SlotForm slotForm, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            //slot을 생성합니다
            Long mySlotId = slotService.createMySlot(slotForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(new SlotIdResponse(mySlotId),"슬롯 생성 성공"));
        } catch (NotValidException e){
            log.error(e.getMessage());
            return ResponseEntity.ok().body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (NotFoundUserException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "My slot 검색 api", description = "개인 slot을 검색하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 검색 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserSlotResponseForm.class)
            )),
            @ApiResponse(responseCode = "406", description = "다른 유저의 슬롯 검색으로 인한 에러"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @GetMapping("/{slotId}")
    public ResponseEntity<ResponseForm<?>> getMySlot(@PathVariable Long slotId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            UserSlotResponseForm responseForm = slotService.findMySlot(slotId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(responseForm,"슬롯 요청 성공"));
        } catch (AccessDeniedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (NotFoundUserException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "My slot 수정 api", description = "개인 slot을 수정하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 수정 성공"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @PutMapping("/{slotId}")
    public ResponseEntity<ResponseForm<?>> updateMySLot(
            @PathVariable Long slotId,
            @RequestBody SlotForm slotForm,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        try {
            SlotIdResponse form = slotService.updateMySlot(slotId, slotForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(form, "수정 성공"));
        } catch (NotFoundUserException | NotFoundContentsException | DateTimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "슬롯 진행 status 수정 api", description = "그룹 슬롯의 에디터가 슬롯 상태를 수정하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 슬롯 상태 변경 성공"),
            @ApiResponse(responseCode = "406", description = "유저 권한 부족")
    })
    @DeleteMapping("/{slotId}")
    public ResponseEntity<ResponseForm<?>> deleteGroupSlot (
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        try {
            slotService.deleteMySlot(slotId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(null,"슬롯 삭제 성공"));
        } catch (AccessDeniedException | NotFoundContentsException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Schema(description = "그룹 슬롯 응답 DTO")
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSlotResponseForm {

        private Long slotId;
        @Schema(description = "제목", example = "스터디 일정")
        private String title;

        @Schema(description = "내용", example = "자바 스터디 모임")
        private String content;

        @Schema(description = "시작 시간", example = "2025-06-01T10:00:00")
        private LocalDateTime startDate;

        @Schema(description = "종료 시간", example = "2025-06-01T12:00:00")
        private LocalDateTime endDate;

        @Schema(description = "생성 시간", example = "2025-05-31T09:00:00")
        private LocalDateTime createDate;

        @Schema(description = "장소", example = "카페 드롭탑")
        private String place;

        @Schema(description = "중요도", example = "HIGH")
        private String importance;

        @Schema(description = "상태", example = "CONFIRMED")
        @NotBlank
        private String status;
    }
}
