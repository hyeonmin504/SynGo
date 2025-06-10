package backend.synGo.controller;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.responseForm.SchedulerForm;
import backend.synGo.service.GroupSchedulerService;
import backend.synGo.service.UserSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class SchedulerController {

    private final UserSchedulerService userSchedulerService;
    private final GroupSchedulerService groupSchedulerService;

    @Operation(summary = "스케줄러 메타 데이터 요청 API", description = "스케줄러 데이터를 가져옵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공")
    })
    @GetMapping("/my/scheduler")
    public ResponseEntity<ResponseForm<?>> getMyScheduler(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SchedulerForm myScheduler = userSchedulerService.getMyScheduler(userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(myScheduler, "my scheduler data 요청 성공"));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.unauthorizedResponse(null,e.getMessage()));
        }
    }

    @Operation(summary = "스케줄러 메타 데이터 요청 API", description = "스케줄러 데이터를 가져옵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요청 성공")
    })
    @GetMapping("/groups/{groupId}/scheduler")
    public ResponseEntity<ResponseForm<?>> getGroupScheduler(@PathVariable Long groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            SchedulerForm myScheduler = groupSchedulerService.getMyScheduler(userDetails.getUserId(),groupId);
            return ResponseEntity.ok(ResponseForm.success(myScheduler, "group scheduler data 요청 성공"));
        } catch (NotFoundUserException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.unauthorizedResponse(null,e.getMessage()));
        }
    }
}
