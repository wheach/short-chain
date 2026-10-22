package com.shortchain.common.exception;

import com.shortchain.common.ResultCode;
import com.shortchain.entity.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：按 ResultCode 的 httpStatus 返回对应 HTTP 状态码
     * （404 不存在 / 429 限流 / 503 熔断 / 400 参数非法）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result> handleBusinessException(BusinessException e) {
        ResultCode rc = e.getResultCode();
        return ResponseEntity.status(rc.getHttpStatus())
                .body(Result.fail(rc));
    }

    /**
     * @Valid 参数校验失败：统一成 Result 结构，返回 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst().orElse(ResultCode.PARAM_ERROR.getMessage());
        return ResponseEntity.status(ResultCode.PARAM_ERROR.getHttpStatus())
                .body(new Result(ResultCode.PARAM_ERROR.getCode(), msg, null));
    }
}
