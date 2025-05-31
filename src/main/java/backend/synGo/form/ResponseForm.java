package backend.synGo.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "공통 응답 포맷")
public class ResponseForm<T> {

    @Schema(description = "성공 여부", example = "true")
    private int code;

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
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
