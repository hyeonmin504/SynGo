package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.exception.AccessDeniedException;
import backend.synGo.exception.NotFoundContentsException;
import backend.synGo.exception.NotValidException;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import backend.synGo.form.responseForm.UserGroupResponseForm;
import backend.synGo.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupBasicController {

    private final GroupService groupService;

    @Operation(summary = "기본 group 생성 api", description = "그룹을 생성하고 기본 스케줄러까지 생성하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공"),
            @ApiResponse(responseCode = "406", description = "패스워드 문제로 인한 에러")
    })
    @PostMapping("/")
    public ResponseEntity<ResponseForm<?>> createGroup(
            @Validated @RequestBody GroupRequestForm requestForm,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        try {
            Long userGroupId =  groupService.createGroupAndReturnUserGroupId(requestForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(new UserGroupResponseForm(userGroupId), "그룹 생성 성공"));
        } catch (NotValidException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "모든 group 정보 요청 api", description = "그룹 정보를 Pageable로 요청하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 데이터 요청 성공"),
            @ApiResponse(responseCode = "200", description = "그룹 데이터 없음")
    })
    @GetMapping("/")
    public ResponseEntity<ResponseForm<?>> getAllGroup(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "12") int size) {
        try {
            //Pageable 객체 생성
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
            return ResponseEntity.status(HttpStatus.OK).body(ResponseForm.success(groupService.findGroups(pageable), "정보 요청 성공"));
        } catch (NotFoundContentsException e) {
            ResponseForm<AllGroupDataResponse> body = ResponseForm.notFoundResponse(new AllGroupDataResponse(), "데이터 없음");
            return ResponseEntity.status(HttpStatus.OK).body(body);
        }
    }

    @Operation(summary = "특정 group 정보 요청 api", description = "선택한 그룹의 정보 요청 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 요청 성공"),
            @ApiResponse(responseCode = "404", description = "해당 유저의 그룹이 없습니다")
    })
    @GetMapping("/{userGroupId}")
    public ResponseEntity<ResponseForm<?>> getGroup(@PathVariable Long userGroupId) {
        try {
            return ResponseEntity.ok().body(ResponseForm.success(groupService.findGroupByGroupId(userGroupId), "정보 요청 성공"));
        } catch (NotFoundContentsException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "그룹 참여 api", description = "그룹에 참여하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공"),
            @ApiResponse(responseCode = "406", description = "패스워드 문제로 인한 에러")
    })
    @PostMapping("/{userGroupId}")
    public ResponseEntity<ResponseForm<?>> joinGroup(
            @PathVariable Long userGroupId,
            @RequestBody JoinGroupForm joinGroupFrom,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            groupService.joinGroup(userGroupId, joinGroupFrom, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(null, "정보 요청 성공"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.ok().body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AllGroupDataResponse {
        private List<GroupsPagingForm> groupsPagingForms;
        private long offset;
        private int pageNum;
        private int numberOfElements;
        private int size;
        private boolean isLast;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GroupForm {
        private Long userGroupId;
        private LocalDateTime createDate;
        private String name;
        private String information;
        private String nickname;
        private int count;
    }
}
