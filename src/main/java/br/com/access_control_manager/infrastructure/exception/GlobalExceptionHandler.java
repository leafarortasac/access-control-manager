package br.com.access_control_manager.infrastructure.exception;

import com.br.shared.contracts.model.ErrorResponseRepresentation;
import com.br.shared.contracts.model.ErrorDetailRepresentation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_ID_KEY = "requestId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseRepresentation> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<ErrorDetailRepresentation> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> {
                    var detail = new ErrorDetailRepresentation();
                    detail.setField(err.getField());
                    detail.setMessage(err.getDefaultMessage());
                    return detail;
                })
                .collect(Collectors.toList());

        return buildErrorResponse("Validation failed", HttpStatus.BAD_REQUEST, request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseRepresentation> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        List<ErrorDetailRepresentation> details = ex.getConstraintViolations().stream()
                .map(violation -> {
                    var detail = new ErrorDetailRepresentation();
                    String propertyPath = violation.getPropertyPath().toString();
                    String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);

                    detail.setField(fieldName);
                    detail.setMessage(violation.getMessage());
                    return detail;
                })
                .collect(Collectors.toList());

        return buildErrorResponse("Validation failed", HttpStatus.BAD_REQUEST, request, details);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseRepresentation> handleBusinessException(
            BusinessException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseRepresentation> handleNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
    }

    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponseRepresentation> handleUnauthorized(
            Exception ex, WebRequest request) {
        String msg = ex instanceof BadCredentialsException ? "Credenciais inválidas" : ex.getMessage();
        return buildErrorResponse(msg, HttpStatus.UNAUTHORIZED, request, null);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponseRepresentation> handleForbidden(
            Exception ex, WebRequest request) {
        String msg = (ex.getMessage() != null && ex.getMessage().contains("senha"))
                ? ex.getMessage()
                : "Acesso negado. Você não tem permissão para este recurso.";
        return buildErrorResponse(msg, HttpStatus.FORBIDDEN, request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseRepresentation> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        String message = String.format("O método %s não é suportado para esta rota. Métodos suportados: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());

        return buildErrorResponse(message, HttpStatus.METHOD_NOT_ALLOWED, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseRepresentation> handleGeneric(Exception ex, WebRequest request) {
        log.error("[Fatal Error] RequestId: {} | Erro: ", MDC.get(REQUEST_ID_KEY), ex);
        return buildErrorResponse("Ocorreu um erro interno no servidor.", HttpStatus.INTERNAL_SERVER_ERROR, request, null);
    }

    private ResponseEntity<ErrorResponseRepresentation> buildErrorResponse(
            String message, HttpStatus status, WebRequest request, List<ErrorDetailRepresentation> details) {

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        var response = new ErrorResponseRepresentation();

        response.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(servletRequest.getRequestURI());
        response.setCode(status.name());

        if (details != null && !details.isEmpty()) {
            response.setDetails(details);
        }

        return ResponseEntity.status(status).body(response);
    }
}