package backend.synGo.login;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestParam String name, @RequestParam String password) {
        LoginResponseDto login = loginService.login(name, password);
        return ResponseEntity.ok(login);
    }

    @PostMapping("/signin")
    public ResponseEntity signin(@RequestParam String password) {
        loginService.signin(password);
        return ResponseEntity.ok("good");
    }

    @GetMapping("/nice")
    public ResponseEntity nice() {
        return ResponseEntity.ok("nice");
    }
}
