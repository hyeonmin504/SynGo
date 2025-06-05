package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.SlotForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.service.GroupSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
