package backend.synGo.controller.my;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.MySlotForm;
import backend.synGo.form.responseForm.MySlotResponseForm;
import backend.synGo.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Slf4j
@RequestMapping("/api/my/scheduler/slots")
@RequiredArgsConstructor
public class MySlotController {

    private final SlotService slotService;

//    @PostMapping("{date}")
//    public ResponseEntity<ResponseForm<?>> generateDateContent(@PathVariable LocalDateTime dateTime, @RequestBody )

    @Operation(summary = "slot 생성 api", description = "개인 slot을 생성하고 date에 맵핑하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 생성 성공"),
            @ApiResponse(responseCode = "406", description = "날자 오류로 인한 에러")
    })
    @PostMapping("/")
    public ResponseEntity<ResponseForm<?>> createMySlot(@Validated @RequestBody MySlotForm mySlotForm, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            //slot을 생성합니다
            Long mySlotId = slotService.createMySlot(mySlotForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(new SlotIdResponse(mySlotId),"슬롯 생성 성공"));
        } catch (NotValidException e){
            log.error(e.getMessage());
            return ResponseEntity.ok().body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (NotFoundUserException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.success(null,e.getMessage()));
        }
    }

    @Operation(summary = "My slot 검색 api", description = "개인 slot을 검색하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "슬롯 검색 성공", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MySlotResponseWrapper.class)
            )),
            @ApiResponse(responseCode = "406", description = "다른 유저의 슬롯 검색으로 인한 에러"),
            @ApiResponse(responseCode = "404", description = "date에 userId 값이 미 할당(그룹 슬롯 요청 에러)")
    })
    @GetMapping("/{slotId}")
    public ResponseEntity<ResponseForm<?>> getMySlot(@PathVariable Long slotId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            MySlotResponseForm responseForm = slotService.findMySlot(slotId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(responseForm,"슬롯 요청 성공"));
        } catch (AccessDeniedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        } catch (NotFoundUserException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notAcceptResponse(null,e.getMessage()));
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SlotIdResponse {
        private Long slotId;
    }

    @Schema(description = "슬롯 요청 성공 응답 래퍼")
    public static class MySlotResponseWrapper extends ResponseForm<MySlotResponseForm> {
        public MySlotResponseWrapper() {
            super(HttpStatus.OK.value(), new MySlotResponseForm(
                    "스터디 일정",
                    "자바 스터디 모임",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now(),
                    "카페 드롭탑",
                    "보통",
                    "PLAN"
            ),"슬롯 요청 성공");
        }
    }
}
