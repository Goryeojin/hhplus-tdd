package io.hhplus.tdd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
class ApiControllerAdvice extends ResponseEntityExceptionHandler {
    // 서버 에러
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("error: ", e);
        return ResponseEntity.internalServerError().body(new ErrorResponse("500", "서버 에러가 발생했습니다."));
    }

    // 잘못된 범위의 숫자를 입력 받았을 때
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse("400", e.getMessage()));
    }

    // PathVariable type mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Method argument type mismatch: ", e);
        String message = String.format("'%s'의 값 '%s'은 잘못된 요청 값입니다.",
                e.getName(), e.getValue());
        return ResponseEntity.badRequest().body(new ErrorResponse("400", message));
    }
}
