package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;
import uk.gov.hmcts.ccd.validators.ValidationError;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final AppInsights appInsights;

    @Autowired
    public RestExceptionHandler(AppInsights appInsights) {
        this.appInsights = appInsights;
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleApiException(final HttpServletRequest request, final ApiException exception) {
        LOG.error(exception.getMessage(), exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails())
            .withCallbackErrors(exception.getCallbackErrors())
            .withCallbackWarnings(exception.getCallbackWarnings());
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(BadSearchRequest.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleSearchRequestException(HttpServletRequest request, Exception exception) {
        LOG.warn(exception.getMessage(), exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<List<ValidationError>> customHandleNotFound(ConstraintViolationException constraintViolationException, WebRequest request) {

        return new ResponseEntity<>(toMessages(constraintViolationException.getConstraintViolations()), HttpStatus.BAD_REQUEST);

    }

    private static List<ValidationError> toMessages(Set<? extends ConstraintViolation<?>> constraintViolations) {
        return constraintViolations.stream()
            .map(constraintViolation -> {
                    if (constraintViolation == null) {
                        return null;
                    } else {
                        final ValidationError error = new ValidationError();
                        error.setTimestamp(LocalDateTime.now());
                        error.setError(constraintViolation.getPropertyPath() + " : " + constraintViolation.getMessage());
                        return error;
                    }
                }
            )
            .collect(Collectors.toList());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleException(final HttpServletRequest request, final Exception exception) {
        LOG.error(exception.getMessage(), exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }
}
