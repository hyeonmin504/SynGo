package backend.synGo.exception.handler;

import backend.synGo.exception.NotValidException;
import backend.synGo.form.ResponseForm;
import backend.synGo.exception.AuthenticationFailedException;
import backend.synGo.exception.NotFoundUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Access Denied");
        return ResponseEntity.status(HttpStatus.OK).body(error);
    }

    @ExceptionHandler({
            NotFoundUserException.class,
            AuthenticationFailedException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ResponseForm<Map<String, Object>>> authException(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED);
        body.put("error", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("method", request.getMethod());

        log.error("error",ex);
        ResponseForm<Map<String, Object>> responseForm = new ResponseForm<>(HttpStatus.UNAUTHORIZED.value(), body, "올바른 입력값이 아닙니다");
        return new ResponseEntity<>(responseForm, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class,
            EmptyResultDataAccessException.class,
            MethodArgumentNotValidException.class,
            UnexpectedTypeException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            NotValidException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ResponseForm<Map<String, Object>>> handleBadRequest(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        log.error("error",ex);
        ResponseForm<Map<String, Object>> responseForm = new ResponseForm<>(HttpStatus.BAD_REQUEST.value(), body, "올바른 입력값이 아닙니다");
        return new ResponseEntity<>(responseForm, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ResponseForm<Map<String, Object>>> handleNotFound(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        log.error("error",ex);
        ResponseForm<Map<String, Object>> responseForm = new ResponseForm<>(HttpStatus.NOT_FOUND.value(), body, "올바른 경로가 아닙니다");
        return new ResponseEntity<>(responseForm, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            ConnectException.class, // db 연결 장애
            SocketTimeoutException.class, //api 연결 시간 장애
            UnknownHostException.class
    })
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ResponseEntity<ResponseForm<Map<String, Object>>> handleNetworkException(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        log.error("error", ex);
        ResponseForm<Map<String, Object>> responseForm = new ResponseForm<>(HttpStatus.SERVICE_UNAVAILABLE.value(), body, "네트워크 문제로 인해 서비스를 다시 이용해주세요.");
        return new ResponseEntity<>(responseForm, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ResponseForm<Map<String, Object>>> handleGenericException(Exception ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        log.error("error",ex);
        ResponseForm<Map<String, Object>> responseForm = new ResponseForm<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), body, "An unexpected error occurred.");
        return new ResponseEntity<>(responseForm, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
