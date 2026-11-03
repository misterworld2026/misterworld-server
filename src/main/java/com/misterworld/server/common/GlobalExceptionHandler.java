package com.misterworld.server.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultDto> business(BusinessException error) {
        return ResponseEntity.status(error.getStatus())
            .body(new ResultDto(false, error.getMessage(), error.getStatus(), error.getCode()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ResultDto> invalid(Exception error) {
        return ResponseEntity.badRequest().body(new ResultDto(false, "입력 형식을 확인하세요.", 400, "INVALID_REQUEST"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResultDto> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(new ResultDto(false, "중복 또는 데이터 제약 충돌입니다.", 409, "DATA_CONFLICT"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultDto> forbidden(AccessDeniedException error) {
        return ResponseEntity.status(403).body(new ResultDto(false, "접근 권한이 없습니다.", 403, "FORBIDDEN"));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ResultDto> missing(Exception error) {
        return ResponseEntity.status(404).body(new ResultDto(false, "대상을 찾을 수 없습니다.", 404, "NOT_FOUND"));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultDto> method(Exception error) {
        return ResponseEntity.status(405).body(new ResultDto(false, "지원하지 않는 HTTP 메서드입니다.", 405, "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultDto> unexpected(Exception error) {
        return ResponseEntity.internalServerError().body(new ResultDto(false, "서버 오류가 발생했습니다.", 500, "INTERNAL_ERROR"));
    }
}
