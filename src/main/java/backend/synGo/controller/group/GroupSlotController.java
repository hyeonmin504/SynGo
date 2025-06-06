package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.form.responseForm.SlotResponseForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.service.GroupSlotService;
import backend.synGo.util.DateTimeRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static backend.synGo.controller.group.SlotMemberController.*;
import static backend.synGo.form.responseForm.SlotResponseForm.*;

@RestController
@Slf4j
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupSlotController {

    private final GroupSlotService groupSlotService;

    @Operation(summary = "그룹 슬롯 등록 api", description = "특정 그룹에서 슬롯을 등록하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 슬롯 등록 성공"),
            @ApiResponse(responseCode = "200", description = "날자 오류로 인한 에러"),
            @ApiResponse(responseCode = "403", description = "유저 권한 부족"),
    })
    @PostMapping("/{groupId}/slots")
    public ResponseEntity<ResponseForm<?>> createGroupSlot(
             @PathVariable Long groupId,
             @AuthenticationPrincipal CustomUserDetails userDetails,
             @Validated @RequestBody SlotForm slotForm) {
        try {
            Long groupSlotId = groupSlotService.createGroupSlot(groupId, slotForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(new SlotIdResponse(groupSlotId),"그룹 슬롯 등록 성공"));
        } catch (NotValidException e) {
            return ResponseEntity.ok().body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "Group slot 검색 api", description = "그룹 slot을 검색하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 검색 성공"),
            @ApiResponse(responseCode = "404", description = "그룹원 외 유저의 요청 에러"),
            @ApiResponse(responseCode = "404", description = "슬롯 없음 에러")
    })
    @GetMapping("/{groupId}/slots/{slotId}")
    public ResponseEntity<ResponseForm<?>> getGroupSlot(
            @PathVariable Long groupId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SlotResponseForm groupSlotForm = groupSlotService.getGroupSlot(groupId, slotId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(groupSlotForm,"슬롯 요청 성공"));
        } catch (NotFoundContentsException | NotFoundUserException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "Group slot 수정 api", description = "그룹 slot을 수정하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 수정 성공"),
            @ApiResponse(responseCode = "404", description = "그룹원 외 유저의 요청 에러"),
            @ApiResponse(responseCode = "404", description = "슬롯 없음 에러")
    })
    @PutMapping("/{groupId}/slots/{slotId}")
    public ResponseEntity<ResponseForm<?>> updateGroupSlotData(
            @PathVariable Long groupId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SlotUpdateForm form) {
        try {
            SlotIdResponse slotIdResponse = groupSlotService.updateSlotData(groupId, slotId, userDetails.getUserId(), form);
            return ResponseEntity.ok().body(ResponseForm.success(slotIdResponse, "슬롯 업데이트 성공"));
        } catch (NotFoundContentsException | NotFoundUserException | AccessDeniedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "슬롯 진행 상태 저장 api", description = "그룹 슬롯의 에디터가 슬롯 상태를 수정하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 슬롯 상태 변경 성공"),
            @ApiResponse(responseCode = "406", description = "유저 권한 부족")
    })
    @PostMapping("/{groupId}/slots/{slotId}")
    public ResponseEntity<ResponseForm<?>> updateGroupSlotStatusData (
            @PathVariable Long groupId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody GroupSlotStatusForm form) {
        try {
            SlotIdResponse slotIdResponse = groupSlotService.updateSlotStatus(groupId, slotId, userDetails.getUserId(), form);
            return ResponseEntity.ok().body(ResponseForm.success(slotIdResponse, "슬롯 상태 업데이트 성공"));
        } catch (AccessDeniedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Data
    public static class SlotUpdateForm {
        @NotBlank
        private String title;
        private String content;
        @DateTimeRange
        private LocalDateTime startDate;
        @DateTimeRange
        private LocalDateTime endDate;
        private String place;
        @NotBlank
        private SlotImportance importance;
        @NotBlank
        private String status;
    }

    @Data
    public static class SlotMemberUpdateForm {
        Status status;
        List<JoinMemberForm> members = new ArrayList<>();
    }
}
