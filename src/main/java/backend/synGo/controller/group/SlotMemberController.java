package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.slot.SlotPermission;
import backend.synGo.domain.slot.Status;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.responseForm.SlotIdResponse;
import backend.synGo.service.SlotMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/groups")
public class SlotMemberController {

    private final SlotMemberService slotMemberService;

    @Operation(summary = "그룹 슬롯 맴버 등록 api", description = "그룹 슬롯의 맴버를 등록하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 슬롯의 맴버 등록 성공"),
            @ApiResponse(responseCode = "406", description = "그룹원 외 유저 접근 실패")
    })
    @PostMapping("/{groupId}/slots/{slotId}/members")
    public ResponseEntity<ResponseForm<?>> RegisterGroupSlotMember(
            @PathVariable Long groupId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody List<JoinMemberRequestForm> form) {
        try {
            SlotIdResponse slotIdResponse = slotMemberService.registerGroupSlotMember(groupId, userDetails.getUserId(), slotId, form);
            return ResponseEntity.ok().body(ResponseForm.success(slotIdResponse, "슬롯 맴버 등록 성공"));
        } catch (NotFoundContentsException | AccessDeniedException | NotValidException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }


    @Data
    @AllArgsConstructor
    public static class JoinMemberRequestForm {
        @NotBlank
        public Long userGroupId;
        @NotBlank
        public SlotPermission permission;
    }

    @Data
    public static class GroupSlotStatusForm {
        public Status status;
    }
}
