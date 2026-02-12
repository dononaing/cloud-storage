package me.cema.cloud_storage.controllers;

import me.cema.cloud_storage.dto.UserExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.stream.Collectors;

@ControllerAdvice()
public class AuthenticationExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<UserExceptionResponse> handleHttpClientErrorException(HttpClientErrorException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserExceptionResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String combinedMessages = exception.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return ResponseEntity.status(exception.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserExceptionResponse(combinedMessages));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<UserExceptionResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserExceptionResponse(exception.getMessage().split(":")[0]));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<UserExceptionResponse> handleBadCredentialsException(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserExceptionResponse(exception.getMessage()));
    }
}
