package backend.synGo.auth.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseForm {
    private String accessToken;
    private String refreshToken;
}
