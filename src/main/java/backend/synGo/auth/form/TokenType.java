package backend.synGo.auth.form;

import lombok.Getter;

@Getter
public enum TokenType {
    AT("accessToken"), RT("refresh_token"), TOKEN("token"), BL("black_list");

    private final String tokenType;

    TokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
