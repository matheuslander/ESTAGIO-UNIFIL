package br.com.uniaoacabamentos.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException e) {
        String mensagem = e.getReason() == null ? "Não foi possível concluir a operação." : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegridade(DataIntegrityViolationException e) {
        LOG.error("Falha de integridade ao persistir dados.", e);
        return ResponseEntity.badRequest().body(Map.of(
                "erro", "Não foi possível salvar os dados. Verifique se o registro já existe ou possui todos os vínculos obrigatórios."
        ));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleBanco(DataAccessException e) {
        LOG.error("Falha ao acessar o banco de dados.", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "erro", "Não foi possível acessar os dados no momento. Tente novamente."
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(RuntimeException e) {
        String mensagem = mensagemSegura(e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    private String mensagemSegura(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) return "Não foi possível concluir a operação.";
        String normalizada = mensagem.toLowerCase();
        if (normalizada.contains("could not execute statement")
                || normalizada.contains("constraint [")
                || normalizada.contains("insert into ")
                || normalizada.contains("sqlstate")) {
            LOG.error("Erro técnico omitido da resposta da API: {}", mensagem);
            return "Não foi possível salvar os dados. Tente novamente ou revise as informações preenchidas.";
        }
        return mensagem;
    }
}
