package backend.synGo.auth.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseForm {
    private String accessToken;
    private String refreshToken;
}
