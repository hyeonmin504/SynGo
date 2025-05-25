package backend.synGo.auth.controller.form;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginForm {

    @NotBlank
    private String password;

    @NotBlank
    @Email(message = "이메일 형식에 맞지 않습니다")
    private String email;
}
