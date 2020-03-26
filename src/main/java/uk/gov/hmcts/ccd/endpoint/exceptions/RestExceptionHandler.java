package uk.gov.hmcts.ccd.endpoint.exceptions;

import com.jayway.jsonpath.JsonPath;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
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
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    private final AppInsights appInsights;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public RestExceptionHandler(AppInsights appInsights, ObjectMapperService objectMapperService) {
        this.appInsights = appInsights;
        this.objectMapperService = objectMapperService;
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

    @ExceptionHandler(CaseValidationException.class)
    @ResponseBody
    public ResponseEntity<HttpError> handleCaseValidationException(HttpServletRequest request, CaseValidationException exception) {

        // read field IDs from CaseValidationException.details (i.e. avoid using full error details as this may contain user data)
        String detailsJson =  this.objectMapperService.convertObjectToString(exception.getDetails());
        List<String> fieldIds = JsonPath.read(detailsJson, "$..field_errors[*].id");

        Map<String, String> customProperties = new HashMap<>();
        customProperties.put("CaseValidationError field IDs", Arrays.toString(fieldIds.toArray()));

        LOG.warn("{}: The following list of fields are in an error state: {}", exception.getMessage(), fieldIds, exception);
        appInsights.trackException(exception, customProperties, SeverityLevel.Warning);
        final HttpError<Serializable> error = new HttpError<>(exception, request)
            .withDetails(exception.getDetails());
        return ResponseEntity
            .status(error.getStatus())
            .body(error);
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
