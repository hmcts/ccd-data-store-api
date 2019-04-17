package uk.gov.hmcts.ccd.endpoint.exceptions;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.AppInsights;
import uk.gov.hmcts.ccd.domain.model.common.HttpError;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;

class RestExceptionHandlerTest {

    @Mock
    private AppInsights appInsights;

    @Mock
    private HttpServletRequest request;
    private String details = "details";

    private ArrayList<String> errors = Lists.newArrayList("error1", "error2");
    private ArrayList<String> warnings = Lists.newArrayList("warning1", "warning2");

    private RestExceptionHandler restExceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        restExceptionHandler = new RestExceptionHandler(appInsights);
    }

    @Test
    void shouldHandleApiException() {
        final ApiException exception = new ApiException("msg");
        exception.withDetails(details);
        exception.withErrors(errors);
        exception.withWarnings(warnings);

        ResponseEntity<HttpError> httpErrorResponseEntity = restExceptionHandler.handleApiException(request, exception);

        Mockito.verify(appInsights).trackException(exception);
        Assert.assertThat(httpErrorResponseEntity.getStatusCode(), is(HttpStatus.UNPROCESSABLE_ENTITY));
        Assert.assertThat(httpErrorResponseEntity.getBody().getDetails(), is(details));
        Assert.assertThat(httpErrorResponseEntity.getBody().getCallbackErrors(), is(errors));
        Assert.assertThat(httpErrorResponseEntity.getBody().getCallbackWarnings(), is(warnings));
    }

    @Test
    void shouldHandleSearchRequestException() {
        BadSearchRequestException exception = new BadSearchRequestException("Bad Search Exception");

        ResponseEntity<HttpError> httpErrorResponseEntity = restExceptionHandler.handleSearchRequestException(request, exception);

        Mockito.verify(appInsights).trackException(exception);
        Assert.assertThat(httpErrorResponseEntity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldHandleGeneralException() {
        EventTokenException exception = new EventTokenException("Event Token Exception");

        ResponseEntity<HttpError> httpErrorResponseEntity = restExceptionHandler.handleException(request, exception);

        Mockito.verify(appInsights).trackException(exception);
        Assert.assertThat(httpErrorResponseEntity.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }
}
