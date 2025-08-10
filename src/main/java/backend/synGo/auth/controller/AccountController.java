package backend.synGo.auth.controller;

import backend.synGo.auth.dto.request.GoogleLinkRequest;
import backend.synGo.auth.dto.response.GoogleLinkResponse;
import backend.synGo.auth.dto.response.SocialInfoResponse;
import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.service.GoogleLinkService;
import backend.synGo.form.ResponseForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountController {

    private final GoogleLinkService googleLinkService;

    @Operation(summary = "구글 계정 연동", description = "로그인된 사용자가 구글 계정을 연동합니다")
    @PostMapping("/link-google")
    public ResponseEntity<GoogleLinkResponse> linkGoogleAccount(
            @Valid @RequestBody GoogleLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String returnUrl) {

        log.info("구글 계정 연동 요청: userId={}, code={}, returnUrl={}",
                userDetails.getUserId(), request.getCode() != null ? "존재" : "없음", returnUrl);

        GoogleLinkResponse response = googleLinkService.linkGoogleAccount(userDetails, request, returnUrl);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "연동된 소셜 정보 조회", description = "사용자의 연동된 소셜 계정 정보를 조회합니다")
    @GetMapping("/social-info")
    public ResponseEntity<ResponseForm<?>> getSocialInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("소셜 정보 조회 요청: provider={}", userDetails.getProvider());

        SocialInfoResponse response = googleLinkService.getSocialInfo(userDetails);

        return ResponseEntity.ok().body(ResponseForm.success(response));
    }

    @Operation(summary = "구글 OAuth URL 생성", description = "구글 계정 연동을 위한 OAuth URL을 생성합니다")
    @GetMapping("/google-oauth-url")
    public ResponseEntity<String> getGoogleOAuthUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String returnUrl) {

        log.info("구글 OAuth URL 생성 요청: userId={}, returnUrl={}", userDetails.getUserId(), returnUrl);

        String oauthUrl = googleLinkService.generateGoogleOAuthUrl(userDetails.getUserId(), returnUrl);

        return ResponseEntity.ok(oauthUrl);
    }
}