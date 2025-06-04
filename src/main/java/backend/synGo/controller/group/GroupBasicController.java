package backend.synGo.controller.group;

import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.domain.userGroupData.Role;
import backend.synGo.exception.*;
import backend.synGo.form.GroupsPagingForm;
import backend.synGo.form.ResponseForm;
import backend.synGo.form.requestForm.GroupRequestForm;
import backend.synGo.form.requestForm.JoinGroupForm;
import backend.synGo.form.responseForm.GroupIdResponseForm;
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
public class GroupBasicController { //todo: userGroupId -> groupId로 바꾸기

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
            Long groupId =  groupService.createGroupAndReturnUserGroupId(requestForm, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(new GroupIdResponseForm(groupId), "그룹 생성 성공"));
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
    @GetMapping("/{groupId}")
    public ResponseEntity<ResponseForm<?>> getGroup(@PathVariable Long groupId) {
        try {
            return ResponseEntity.ok().body(ResponseForm.success(groupService.findGroupByGroupId(groupId), "정보 요청 성공"));
        } catch (NotFoundContentsException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseForm.notFoundResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "그룹 참여 api", description = "그룹에 참여하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 생성 성공"),
            @ApiResponse(responseCode = "406", description = "패스워드 문제로 인한 에러")
    })
    @PostMapping("/{groupId}")
    public ResponseEntity<ResponseForm<?>> joinGroup(
            @PathVariable Long groupId,
            @RequestBody JoinGroupForm joinGroupFrom,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            GroupIdResponseForm form = groupService.joinGroup(groupId, joinGroupFrom, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(form, "그룹 참여 성공"));
        } catch (ExistUserException | NotFoundContentsException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "그룹 역할 조회 api", description = "그룹원이 그룹원들의 역할을 조회하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 역할 조회 성공"),
            @ApiResponse(responseCode = "406", description = "그룹 역할 조회 실패")
    })
    @GetMapping("/{groupId}/role")
    public ResponseEntity<ResponseForm<?>> getMembersRole(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<UserGroupRoleSummary> membersRole = groupService.getMemberRole(groupId, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(membersRole, "그룹 역할 조회 성공"));
        } catch (AccessDeniedException | NotFoundContentsException e){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "그룹 역할 수정 api", description = "특정 그룹원이 그룹원들의 역할을 수정하는 api")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 역할 수정 성공"),
            @ApiResponse(responseCode = "406", description = "그룹 역할 수정 실패")
    })
    @PostMapping("/{groupId}/role")
    public ResponseEntity<ResponseForm<?>> updateMembersRole(
            @PathVariable Long groupId,
            @RequestBody List<UserGroupRoleSummary> membersRole,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            GroupIdResponseForm form = groupService.updateMembersRole(groupId, membersRole, userDetails.getUserId());
            return ResponseEntity.ok().body(ResponseForm.success(form, "그룹 역할 수정 성공")); //todo: 반환은 groupId로 수정
        } catch (AccessDeniedException | NotFoundContentsException | ExistUserException | NotAllowException e){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ResponseForm.notAcceptResponse(null, e.getMessage()));
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
        private Long groupId;
        private LocalDateTime createDate;
        private String name;
        private String information;
        private String nickname;
        private int count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserGroupRoleSummary {
        private Long id;
        private String nickname;
        private Role role;

        public UserGroupRoleSummary updateRole(Role role) {
            this.role = role;
            return this;
        }
    }
}
