package backend.synGo.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseForm<T> {

    private int code;
    private T data;
    private String message;

    public static <T> ResponseForm<T> success(T data) {
        return new ResponseForm<>(HttpStatus.OK.value(), data, "Ok");
    }

    public static <T> ResponseForm<T> success(T data, String message) {
        return new ResponseForm<>(HttpStatus.OK.value(), data, message);
    }

    public static <T> ResponseForm<T> unauthorizedResponse(T data, String message) {
        return new ResponseForm<>(HttpStatus.UNAUTHORIZED.value(), data, message);
    }

    public static <T> ResponseForm<T> notFoundResponse(T data, String message) {
        return new ResponseForm<>(HttpStatus.NOT_FOUND.value(), data, message);
    }

    public static <T> ResponseForm<T> notAcceptResponse(T data, String message) {
        return new ResponseForm<>(NOT_ACCEPTABLE.value(), data, message);
    }

    public static <T> ResponseForm<T> requestTimeOutResponse(T data, String message) {
        return new ResponseForm<>(REQUEST_TIMEOUT.value(), data, message);
    }
}
