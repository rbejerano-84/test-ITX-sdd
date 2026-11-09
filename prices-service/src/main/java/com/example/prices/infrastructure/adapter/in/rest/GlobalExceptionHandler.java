package com.example.prices.infrastructure.adapter.in.rest;

import com.example.prices.application.PriceNotFoundException;
import com.example.prices.infrastructure.adapter.in.rest.generated.model.FieldError;
import com.example.prices.infrastructure.adapter.in.rest.generated.model.ProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(PriceNotFoundException.class)
  public ResponseEntity<Object> handlePriceNotFound(
      PriceNotFoundException exception, HttpServletRequest request) {
    return problemResponse(
        HttpStatus.NOT_FOUND, "Price Not Found", exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<FieldError> errors =
        exception.getConstraintViolations().stream().map(this::toFieldError).toList();
    return problemResponse(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        "Invalid request parameters",
        request.getRequestURI(),
        errors);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    LOGGER.error("Unexpected exception while processing request", exception);
    return problemResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal Server Error",
        "An unexpected error occurred",
        request.getRequestURI());
  }

  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problemResponse(
        status,
        "Bad Request",
        "Required parameter '" + exception.getParameterName() + "' is missing",
        requestPath(request),
        List.of(new FieldError(exception.getParameterName(), "is required")));
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String parameterName =
        exception instanceof MethodArgumentTypeMismatchException mismatch
            ? mismatch.getName()
            : "parameter";
    return problemResponse(
        status,
        "Bad Request",
        "Invalid value for parameter '" + parameterName + "'",
        requestPath(request),
        List.of(new FieldError(parameterName, "must have a valid value")));
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problemResponse(
        status,
        "Method Not Allowed",
        "Method '" + exception.getMethod() + "' is not supported",
        requestPath(request),
        List.of(),
        headers);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problemResponse(
        status, "Not Acceptable", "Could not find acceptable representation", requestPath(request));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problemResponse(
        status,
        "Unsupported Media Type",
        "The request media type is not supported",
        requestPath(request));
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail contractBody;
    if (body instanceof org.springframework.http.ProblemDetail springProblemDetail) {
      contractBody =
          new ProblemDetail(
                  springProblemDetail.getTitle() == null
                      ? titleFor(status)
                      : springProblemDetail.getTitle(),
                  status.value())
              .detail(
                  status.is5xxServerError()
                      ? "An unexpected error occurred"
                      : nonEmptyDetail(springProblemDetail.getDetail()))
              .instance(requestPath(request));
    } else {
      contractBody =
          new ProblemDetail(titleFor(status), status.value())
              .detail(
                  status.is5xxServerError()
                      ? "An unexpected error occurred"
                      : "Request could not be processed")
              .instance(requestPath(request));
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.putAll(headers);
    responseHeaders.setContentType(PROBLEM_JSON);
    return super.createResponseEntity(contractBody, responseHeaders, status, request);
  }

  private FieldError toFieldError(ConstraintViolation<?> violation) {
    String field = "parameter";
    for (jakarta.validation.Path.Node node : violation.getPropertyPath()) {
      field = node.getName();
    }
    return new FieldError(field, violation.getMessage());
  }

  private ResponseEntity<Object> problemResponse(
      HttpStatusCode status, String title, String detail, String instance) {
    return problemResponse(status, title, detail, instance, List.of());
  }

  private ResponseEntity<Object> problemResponse(
      HttpStatusCode status,
      String title,
      String detail,
      String instance,
      List<FieldError> errors) {
    return problemResponse(status, title, detail, instance, errors, new HttpHeaders());
  }

  private ResponseEntity<Object> problemResponse(
      HttpStatusCode status,
      String title,
      String detail,
      String instance,
      List<FieldError> errors,
      HttpHeaders headers) {
    ProblemDetail problem =
        new ProblemDetail(title, status.value()).detail(detail).instance(instance).errors(errors);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.putAll(headers);
    responseHeaders.setContentType(PROBLEM_JSON);
    return new ResponseEntity<>(problem, responseHeaders, status);
  }

  private String requestPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return request.getDescription(false).replaceFirst("^uri=", "");
  }

  private String titleFor(HttpStatusCode status) {
    HttpStatus resolved = HttpStatus.resolve(status.value());
    return resolved == null ? "HTTP Error" : resolved.getReasonPhrase();
  }

  private String nonEmptyDetail(String detail) {
    return detail == null ? "Request could not be processed" : detail;
  }
}
