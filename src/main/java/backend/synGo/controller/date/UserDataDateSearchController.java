package backend.synGo.controller.date;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.form.DateDtoForDay;
import backend.synGo.form.DateDtoForMonth;
import backend.synGo.form.ResponseForm;
import backend.synGo.service.date.user.DateUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/my")
public class UserDataDateSearchController {

    private final DateUserService dateService;

    @Operation(summary = "개인 월 슬롯 데이터 조회 api", description = "개인 date를 한달 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "한달 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/date/month")
    public ResponseEntity<ResponseForm<?>> getUserDataForMonth(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            List<DateDtoForMonth> dates = dateService.getUserDataDatesForMonth(requestYear, requestMonth, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "개인 데이터 조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "개인 월 그룹 슬롯 데이터 조회 api", description = "그룹에 개인이 속하면서 개인이 할당된 date를 한달 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "한달 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/groups/date/month")
    public ResponseEntity<ResponseForm<?>> getUserByGroupDataForMonth(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            List<DateDtoForMonth> dates = dateService.getUserDataDatesForMonthByGroup(requestYear, requestMonth, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "개인의 그룹 데이터 조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "개인 일 슬롯 데이터 조회 api", description = "유저의 그룹 date를 하루 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 하루 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/date/day")
    public ResponseEntity<ResponseForm<?>> getUserDataForDay(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @Min(1) @Max(31) Integer day,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();
        int requestDay = (day != null) ? day : now.getDayOfMonth();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            DateDtoForDay dates = dateService.getUserDataDatesForDay(requestYear, requestMonth, requestDay, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "개인 일 그룹 슬롯 데이터 조회 api", description = "유저의 그룹 date를 하루 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 하루 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/groups/date/day")
    public ResponseEntity<ResponseForm<?>> getUserByGroupDataForDay(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @Min(1) @Max(31) Integer day,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();
        int requestDay = (day != null) ? day : now.getDayOfMonth();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            DateDtoForDay dates = dateService.getUserDataDatesForDayByGroup(requestYear, requestMonth, requestDay, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }
}
