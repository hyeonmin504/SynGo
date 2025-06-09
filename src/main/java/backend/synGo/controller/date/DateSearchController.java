package backend.synGo.controller.date;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.slot.SlotImportance;
import backend.synGo.domain.slot.Status;
import backend.synGo.form.ResponseForm;
import backend.synGo.service.DateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.*;
;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/groups")
public class DateSearchController {

    private final DateService dateService;

    @Operation(summary = "그룹 슬롯 등록 api", description = "특정 그룹에서 슬롯을 등록하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 슬롯 등록 성공"),
            @ApiResponse(responseCode = "200", description = "날자 오류로 인한 에러"),
            @ApiResponse(responseCode = "403", description = "유저 권한 부족"),
    })
    @GetMapping("/{groupId}/date")
    public ResponseEntity<ResponseForm<?>> getGroupDateData(
            @PathVariable Long groupId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LocalDate now = LocalDate.now();
        int requestYear = (year != null) ? year : now.getYear();
        int requestMonth = (month != null) ? month : now.getMonthValue();

        try {
            log.info("date={},{}", requestYear, requestMonth);
            GetGroupDateInfo dates = dateService.getDatesForMonthInGroup(groupId, requestYear, requestMonth, userDetails.getUserId());
            return ResponseEntity.ok(ResponseForm.success(dates, "조회 성공"));
        } catch (Exception e) {
            log.error("그룹 날짜 조회 오류: {}", e.getMessage(), e);
            return ResponseEntity.ok(ResponseForm.success(null, e.getMessage()));
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GetGroupDateInfo {
        Long groupId;
        @Builder.Default
        List<DateInfo> dateInfo = new ArrayList<>();
    }

    @Data
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class DateInfo {
        Long dateId;
        int slotCount;
        LocalDate today;
        @Builder.Default
        List<SlotInfo> slotInfo = new ArrayList<>();

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SlotInfo {
        Long slotId;
        String title;
        LocalDateTime startTime;
        Status status;
        SlotImportance importance;
    }
}
