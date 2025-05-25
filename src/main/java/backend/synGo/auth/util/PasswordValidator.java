package backend.synGo.auth.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class PasswordValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[a-z])(?=.*[!@#$%^&*()-+=]).{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password) {
        log.info("password={}", password);
        if (password == null) {
            return false;
        }
        return pattern.matcher(password).matches();
    }
}