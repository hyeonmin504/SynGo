package backend.synGo.controller.date;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.form.DateDtoForDay;
import backend.synGo.form.DateDtoForMonth;
import backend.synGo.form.GroupDateInfo;
import backend.synGo.form.ResponseForm;
import backend.synGo.service.date.group.DateInGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static backend.synGo.controller.date.UserDataDateSearchController.*;
import static backend.synGo.service.GroupSlotService.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/groups")
public class GroupDateSearchController {

    private final DateInGroupService dateService;

    @Operation(summary = "그룹 슬롯 한달 데이터 조회 api", description = "그룹 date를 한달 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 한달 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/{groupId}/date/month")
    public ResponseEntity<ResponseForm<?>> getGroupMonthDateData(
            @PathVariable Long groupId,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            List<DateDtoForMonth> dates = dateService.getDatesForMonthInGroup(groupId, requestYear, requestMonth, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "그룹 슬롯 하루 데이터 조회 api", description = "그룹 date를 하루 간격으로 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 하루 date,slot 조회 성공"),
            @ApiResponse(responseCode = "406", description = "잘못된 유저 요청"),
    })
    @GetMapping("/{groupId}/date/day")
    public ResponseEntity<ResponseForm<?>> getGroupDayDateData(
            @PathVariable Long groupId,
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
            DateDtoForDay dates = dateService.findGroupDataByDayInGroup(groupId, requestYear, requestMonth, requestDay, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "조회 성공"));
        } catch (DateTimeException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }
}
