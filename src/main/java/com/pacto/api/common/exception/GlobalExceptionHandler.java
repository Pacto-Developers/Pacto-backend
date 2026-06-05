package com.pacto.api.common.exception;

import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.escrow.exception.InvalidEscrowStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<?> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<?> handleInvalidPassword(InvalidPasswordException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler({EmailNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<?> handleAuthNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<?> handleInsufficientBalance(InsufficientBalanceException e) {
        return ResponseEntity.badRequest()
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<?> handleWalletNotFound(WalletNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(EscrowNotFoundException.class)
    public ResponseEntity<?> handleEscrowNotFound(EscrowNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<?> handlePaymentNotFound(PaymentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(PaymentAlreadyProcessedException.class)
    public ResponseEntity<?> handlePaymentAlreadyProcessed(PaymentAlreadyProcessedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<?> handlePaymentVerification(PaymentVerificationException e) {
        return ResponseEntity.badRequest()
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(PortOneApiException.class)
    public ResponseEntity<?> handlePortOneApiException(PortOneApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(InvalidEscrowStateException.class)
    public ResponseEntity<?> handleInvalidEscrowState(InvalidEscrowStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(CampaignSlotFullException.class)
    public ResponseEntity<?> handleCampaignSlotFull(CampaignSlotFullException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.internalServerError()
                .body(CommonResponse.failure("서버 오류가 발생했습니다."));
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<?> handleCampaignNotFound(CampaignNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(MissionNotFoundException.class)
    public ResponseEntity<?> handleMissionNotFound(MissionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.failure(e.getMessage()));
    }

    @ExceptionHandler(InvalidMissionStatusException.class)
    public ResponseEntity<?> handleInvalidMissionStatus(InvalidMissionStatusException e) {
        return ResponseEntity.badRequest()
                .body(CommonResponse.failure(e.getMessage()));
    }
}
