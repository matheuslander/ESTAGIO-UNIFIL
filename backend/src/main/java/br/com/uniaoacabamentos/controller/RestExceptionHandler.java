package br.com.uniaoacabamentos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }
}
