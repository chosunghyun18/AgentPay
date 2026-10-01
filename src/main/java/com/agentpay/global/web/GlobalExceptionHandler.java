package com.agentpay.global.web;

import com.agentpay.global.domain.DomainException;
import com.agentpay.global.domain.ForbiddenException;
import com.agentpay.global.domain.NotFoundException;
import com.agentpay.payment.application.PaymentProcessorException;
import com.agentpay.payment.domain.IdempotencyConflictException;
import com.agentpay.payment.domain.InvalidStateTransitionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return of(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbidden(ForbiddenException e) {
        return of(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler({IdempotencyConflictException.class, InvalidStateTransitionException.class})
    public ResponseEntity<ErrorResponse> conflict(DomainException e) {
        return of(HttpStatus.CONFLICT, e);
    }

    /** 같은 Idempotency-Key 동시 요청 — 유니크 제약으로 한 건만 성공한다. 재시도하면 기존 결과를 받는다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONCURRENT_REQUEST", "동시 요청 충돌입니다. 같은 Idempotency-Key로 재시도하세요."));
    }

    @ExceptionHandler(PaymentProcessorException.class)
    public ResponseEntity<ErrorResponse> processor(PaymentProcessorException e) {
        return of(HttpStatus.BAD_GATEWAY, e);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> domain(DomainException e) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, e);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            MissingRequestHeaderException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }

    private static ResponseEntity<ErrorResponse> of(HttpStatus status, DomainException e) {
        return ResponseEntity.status(status).body(new ErrorResponse(e.code(), e.getMessage()));
    }
}
