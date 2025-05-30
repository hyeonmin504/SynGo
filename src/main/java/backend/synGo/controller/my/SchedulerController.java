package backend.synGo.controller.my;

import backend.synGo.domain.schedule.UserScheduler;
import backend.synGo.exception.NotFoundUserException;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.responseForm.MySchedulerForm;
import backend.synGo.service.UserSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class SchedulerController {

    private final UserSchedulerService userSchedulerService;

    @Operation(summary = "스케줄러 메타 데이터 요청 API", description = "스케줄러 데이터를 가져옵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기본 토큰 삭제 및 access Token만 재발급"),
    })
    @GetMapping("/scheduler")
    public ResponseEntity<ResponseForm<?>> getMyScheduler(HttpServletRequest request) {
        try {
            MySchedulerForm myScheduler = userSchedulerService.getMyScheduler(request);
            return ResponseEntity.ok(ResponseForm.success(myScheduler, "my scheduler data 요청 성공"));
        } catch (NotFoundUserException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.unauthorizedResponse(null,e.getMessage()));
        }
    }
}
