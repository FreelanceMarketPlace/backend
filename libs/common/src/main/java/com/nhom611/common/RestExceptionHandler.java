package com.nhom611.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException ex, WebRequest request) {
		log.warn("Malformed JSON request at {}: {}", extractPath(request), ex.getMessage());
		ApiError body = ApiError.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Malformed JSON request",
				extractPath(request),
				null
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
	public ResponseEntity<ApiError> handleDuplicateKey(Exception ex, WebRequest request) {
		log.warn("Duplicate resource at {}: {}", extractPath(request), ex.getMessage());
		ApiError body = ApiError.of(
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"Duplicate resource",
				extractPath(request),
				null
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
		log.warn("Validation failed at {}: {}", extractPath(request), ex.getMessage());
		Map<String, Object> details = new LinkedHashMap<>();
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fields.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		details.put("fields", fields);

		ApiError body = new ApiError(
				java.time.Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Validation failed",
				extractPath(request),
				null,
				details
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, WebRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		if (status.is5xxServerError()) {
			log.error("Server error at {}: {}", extractPath(request), ex.getReason(), ex);
		} else {
			log.warn("Request rejected at {}: {} {}", extractPath(request), status.value(), ex.getReason());
		}

		ApiError body = ApiError.of(
				status.value(),
				status.getReasonPhrase(),
				ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
				extractPath(request),
				null
		);
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ResponseEntity<ApiError> handleErrorResponse(ErrorResponseException ex, WebRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		if (status.is5xxServerError()) {
			log.error("Error response at {}: {}", extractPath(request), ex.getMessage(), ex);
		} else {
			log.warn("Error response at {}: {}", extractPath(request), ex.getMessage());
		}
		ApiError body = ApiError.of(
				status.value(),
				status.getReasonPhrase(),
				ex.getBody() != null && ex.getBody().getDetail() != null ? ex.getBody().getDetail() : status.getReasonPhrase(),
				extractPath(request),
				null
		);
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, WebRequest request) {
		log.error("Unexpected error at {}", extractPath(request), ex);
		ApiError body = ApiError.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"Unexpected error",
				extractPath(request),
				null
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private static String extractPath(WebRequest request) {
		if (request == null) {
			return null;
		}
		String desc = request.getDescription(false);
		if (desc == null) {
			return null;
		}
		// Typical: "uri=/path"
		int idx = desc.indexOf("uri=");
		if (idx >= 0) {
			return desc.substring(idx + 4);
		}
		return desc;
	}
}

