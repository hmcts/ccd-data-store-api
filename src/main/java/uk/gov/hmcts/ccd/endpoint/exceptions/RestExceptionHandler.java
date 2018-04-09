package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.concurrent.CompletionException;

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
        LOG.warn("data store request processing api exception", exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails())
            .withCallbackErrors(exception.getCallbackErrors())
            .withCallbackWarnings(exception.getCallbackWarnings());
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleException(final HttpServletRequest request, final Exception exception) {
        LOG.error("data store request processing exception", exception);
        appInsights.trackException(exception);
        final HttpError<Serializable> error = new HttpError<>(exception, request);
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
    }

    @ExceptionHandler(CompletionException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleCompletionException(final HttpServletRequest request, final CompletionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof ApiException) {
            return handleApiException(request, (ApiException) cause);
        } else if (cause instanceof Exception) {
            return handleException(request, (Exception) cause);
        }
        return ResponseEntity.status(new HttpError<>(exception, request).getStatus()).build();
    }
}
