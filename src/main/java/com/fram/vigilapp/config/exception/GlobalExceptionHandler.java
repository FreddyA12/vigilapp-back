package com.fram.vigilapp.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        StringBuilder errorMessages = new StringBuilder();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errorMessages
                        .append(error.getDefaultMessage())
                        .append(". ")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.PRECONDITION_FAILED.value());
        response.put("message", errorMessages.toString().trim());
        response.put("error", HttpStatus.PRECONDITION_FAILED.getReasonPhrase());

        return new ResponseEntity<>(response, HttpStatus.PRECONDITION_FAILED);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", ex.getStatusCode().value(),
                        "error", ex.getStatusCode(),
                        "message", Objects.requireNonNull(ex.getReason())
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", HttpStatus.UNAUTHORIZED.value(),
                        "status", HttpStatus.UNAUTHORIZED,
                        "timestamp", LocalDateTime.now(),
                        "message", (ex.getMessage().equals("Bad credentials") ? "Credenciales inválidas" : ex.getMessage())
                ));
    }

    /**
     * Maneja errores cuando falta un parámetro de request obligatorio
     * Ejemplo: @RequestParam String firstName - si no se envía firstName
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        String paramName = ex.getParameterName();
        String paramType = ex.getParameterType();

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.put("message", String.format("El parámetro '%s' es obligatorio y no fue proporcionado", paramName));
        response.put("field", paramName);
        response.put("expectedType", paramType);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores cuando falta una parte de multipart/form-data
     * Ejemplo: @RequestPart MultipartFile fotoCedula - si no se envía el archivo
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingFilePart(MissingServletRequestPartException ex) {
        String partName = ex.getRequestPartName();

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.put("message", String.format("El archivo '%s' es obligatorio y no fue proporcionado", partName));
        response.put("field", partName);
        response.put("hint", "Asegúrese de enviar el archivo en la petición multipart/form-data");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores cuando el tipo de un argumento no coincide
     * Ejemplo: Se espera un Integer pero se envía "abc"
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        Object providedValue = ex.getValue();

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.put("message", String.format(
                "El parámetro '%s' tiene un tipo de dato inválido. Se esperaba '%s' pero se recibió '%s'",
                paramName, expectedType, providedValue));
        response.put("field", paramName);
        response.put("expectedType", expectedType);
        response.put("providedValue", providedValue);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores cuando el cuerpo del request no puede ser leído
     * Ejemplo: JSON mal formado
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());

        String message = "El cuerpo de la petición no puede ser leído o está mal formado";

        // Intentar dar un mensaje más específico si es JSON
        if (ex.getMessage() != null && ex.getMessage().contains("JSON")) {
            message = "El formato JSON es inválido. Verifique la sintaxis del cuerpo de la petición";
        }

        response.put("message", message);
        response.put("hint", "Verifique que el formato de los datos sea correcto");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja cualquier excepción no capturada específicamente
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        response.put("message", "Ha ocurrido un error interno en el servidor");

        // En desarrollo, incluir detalles del error
        // En producción, esto debería estar deshabilitado
        if (ex.getMessage() != null) {
            response.put("details", ex.getMessage());
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
