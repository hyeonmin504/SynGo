package backend.synGo.auth.controller;

import backend.synGo.auth.controller.form.LoginForm;
import backend.synGo.auth.form.TokenResponseForm;
import backend.synGo.form.ResponseForm;
import backend.synGo.auth.controller.form.SignUpForm;
import backend.synGo.auth.service.AuthService;
import backend.synGo.exception.*;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static backend.synGo.form.ResponseForm.*;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "사용자 로그인 API", description = "사용자 이메일과 패스워드를 통해 로그인을 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseForm<?>> login(@Validated @RequestBody LoginForm form, HttpServletRequest request) {
        try {
            return ResponseEntity.ok().body(success(authService.login(form, request), "로그인 성공"));
        } catch (NotFoundUserException | NotValidException e) {

            return ResponseEntity.ok().body(unauthorizedResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "회원가입 API", description = "이름, 이메일, 패스워드, 패스워드 확인을 받아옵니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "406", description = "회원가입 실패")
    })
    @PostMapping("/signup")
    public ResponseEntity<ResponseForm<?>> signup(@Validated @RequestBody SignUpForm form, HttpServletRequest request) {
        try {
            authService.signUp(form, request);
            return ResponseEntity.ok().body(ResponseForm.success(null,"회원가입 성공"));
        } catch (NotValidException | ExistUserException e) {
            return ResponseEntity.ok().body(ResponseForm.notAcceptResponse(form, e.getMessage()));
        }
    }

    @Operation(summary = "로그아웃 API", description = "user_id로 로그아웃")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
    })
    @GetMapping("/logout")
    public ResponseEntity<ResponseForm<?>> logout(HttpServletRequest request) {
        try {
            authService.logout(request);
            return ResponseEntity.ok().body(ResponseForm.success(null,"로그아웃 성공"));
        } catch (ExpiredTokenException | AuthenticationFailedException | JwtException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(ResponseForm.unauthorizedResponse(null, e.getMessage()));
        }
    }

    @Operation(summary = "access 토큰 재발급 API", description = "Refresh Token 을 통해 Access Token을 재발급")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기본 토큰 삭제 및 access Token만 재발급"),
            @ApiResponse(responseCode = "401", description = "토큰 문제로 인한 재발급 실패")
    })
    @GetMapping("reissue")
    public ResponseEntity<ResponseForm<?>> reissue(HttpServletRequest request) {
        try {
            TokenResponseForm token = authService.reissue(request);
            return ResponseEntity.ok(ResponseForm.success(token, "새로운 access token 발급 완료"));
        } catch (ExpiredTokenException | AuthenticationFailedException | JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseForm.unauthorizedResponse(null, e.getMessage()));
        }
    }
}