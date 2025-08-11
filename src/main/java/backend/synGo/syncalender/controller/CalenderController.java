package backend.synGo.syncalender.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.controller.my.MySlotController.UserSlotResponseForm;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundDataException;
import backend.synGo.form.ResponseForm;
import backend.synGo.service.SlotService;
import backend.synGo.syncalender.dto.SyncCalenderRequest;
import backend.synGo.syncalender.service.UserSyncCalenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/syn/calender")
public class CalenderController {

    private final UserSyncCalenderService userSyncCalenderService;
    private final SlotService slotService;

    @PostMapping("/google")
    public ResponseEntity<ResponseForm<?>> syncCalender(
            @RequestBody SyncCalenderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        try {
            //슬롯 조회
            UserSlotResponseForm mySlot = slotService.findMySlot(request.getSlotId(), userDetails.getUserId());
            //캘린더 연동
            userSyncCalenderService.syncToGoogleCalender(mySlot, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(null, "연동 성공"));
        } catch (AccessDeniedException | NotFoundDataException e ) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, "슬롯을 찾을 수 없습니다"));
        }


    }
}
