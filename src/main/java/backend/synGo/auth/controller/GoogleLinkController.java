package backend.synGo.auth.controller;

import backend.synGo.auth.dto.request.GoogleLinkRequest;
import backend.synGo.auth.dto.response.GoogleLinkResponse;
import backend.synGo.auth.dto.response.SocialInfoResponse;
import backend.synGo.auth.form.CustomUserDetails;
import backend.synGo.auth.service.GoogleLinkService;
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
@Tag(name = "소셜 로그인 연동", description = "기존 사용자의 소셜 계정 연동 관련 API")
public class GoogleLinkController {

    private final GoogleLinkService googleLinkService;

    @Operation(summary = "구글 계정 연동", description = "로그인된 사용자가 구글 계정을 연동합니다")
    @PostMapping("/link-google")
    public ResponseEntity<GoogleLinkResponse> linkGoogleAccount(
            @Valid @RequestBody GoogleLinkRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("구글 계정 연동 요청: userId={}, code={}",
                userDetails.getUserId(), request.getCode() != null ? "존재" : "없음");

        GoogleLinkResponse response = googleLinkService.linkGoogleAccount(userDetails, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "연동된 소셜 정보 조회", description = "사용자의 연동된 소셜 계정 정보를 조회합니다")
    @GetMapping("/social-info")
    public ResponseEntity<SocialInfoResponse> getSocialInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("소셜 정보 조회 요청: userId={}", userDetails.getUserId());

        SocialInfoResponse response = googleLinkService.getSocialInfo(userDetails);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "구글 OAuth URL 생성", description = "구글 계정 연동을 위한 OAuth URL을 생성합니다")
    @GetMapping("/google-oauth-url")
    public ResponseEntity<String> getGoogleOAuthUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("구글 OAuth URL 생성 요청: userId={}", userDetails.getUserId());

        String oauthUrl = googleLinkService.generateGoogleOAuthUrl(userDetails.getUserId());

        return ResponseEntity.ok(oauthUrl);
    }
}