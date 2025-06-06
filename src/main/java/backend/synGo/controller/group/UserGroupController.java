package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.form.ResponseForm;
import backend.synGo.service.UserGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static backend.synGo.form.ResponseForm.*;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class UserGroupController {

    private final UserGroupService userGroupService;

    @Operation(summary = "그룹원 정보 조회 api", description = "그룹원이 그룹원 정보를 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹원 조회 성공"),
            @ApiResponse(responseCode = "406", description = "그룹원 외 조회 실패")
    })
    @GetMapping("/{groupId}/member")
    public ResponseEntity<ResponseForm<?>> getMemberInGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<UserInGroupData> groupData = userGroupService.getMember(groupId, userDetails.getUserId());
            return ok().body(success(groupData, "조회 성공"));
        } catch (AccessDeniedException e) {
            return status(HttpStatus.NOT_ACCEPTABLE).body(notAcceptResponse(null, "그룹원 외 조회 실패"));
        }
    }

    @Data
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class UserInGroupData {
        Long userGroupId;
        Role role;
        LocalDateTime joinDate;
        String nickname;
    }
}
