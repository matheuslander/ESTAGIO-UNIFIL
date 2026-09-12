package br.com.uniaoacabamentos.config;

import br.com.uniaoacabamentos.service.AutenticacaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {
    private final AutenticacaoService autenticacaoService;
    private final ObjectMapper objectMapper;

    public AutenticacaoInterceptor(AutenticacaoService autenticacaoService, ObjectMapper objectMapper) {
        this.autenticacaoService = autenticacaoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        try {
            autenticacaoService.exigirUsuarioAutenticado(request);
            return true;
        } catch (ResponseStatusException exception) {
            response.setStatus(exception.getStatusCode().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "erro", exception.getReason() == null ? "Não autenticado." : exception.getReason()
            ));
            return false;
        }
    }
}

