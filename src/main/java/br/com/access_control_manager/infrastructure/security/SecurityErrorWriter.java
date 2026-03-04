package br.com.access_control_manager.infrastructure.security;

import com.br.shared.contracts.model.ErrorResponseRepresentation;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void writeError(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        ErrorResponseRepresentation error = new ErrorResponseRepresentation();
        error.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(message);
        error.setPath(path);
        error.setCode(status.name());

        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}