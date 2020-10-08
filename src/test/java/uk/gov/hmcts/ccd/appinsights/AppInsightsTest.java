package uk.gov.hmcts.ccd.appinsights;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.CALLBACK_DURATION;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.CALLBACK_EVENT_NAME;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.METHOD;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.STATUS;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.TYPE;
import static uk.gov.hmcts.ccd.appinsights.AppInsights.URI;

public class AppInsightsTest {

    @InjectMocks
    private AppInsights classUnderTest;

    @Mock
    private TelemetryClient telemetryClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void trackRequest_shouldUseRequestTelemetry_successfulRequest() {

        // ARRANGE
        String name = "test name 1";
        long duration = 123L;

        // ACT
        classUnderTest.trackRequest(name, duration, true);

        // ASSERT
        ArgumentCaptor<RequestTelemetry> requestTelemetryCaptor = ArgumentCaptor.forClass(RequestTelemetry.class);
        verify(telemetryClient, times(1)).trackRequest(requestTelemetryCaptor.capture());
        RequestTelemetry requestTelemetry = requestTelemetryCaptor.getValue();
        assertThat(requestTelemetry.getName(), is(equalTo(name)));
        assertThat(requestTelemetry.getDuration().getTotalMilliseconds(), is(equalTo(duration)));
        assertTrue(requestTelemetry.isSuccess());
    }

    @Test
    public void trackRequest_shouldUseRequestTelemetry_failedRequest() {

        // ARRANGE
        String name = "test name 2";
        long duration = 321L;

        // ACT
        classUnderTest.trackRequest(name, duration, false);

        // ASSERT
        ArgumentCaptor<RequestTelemetry> requestTelemetryCaptor = ArgumentCaptor.forClass(RequestTelemetry.class);
        verify(telemetryClient, times(1)).trackRequest(requestTelemetryCaptor.capture());
        RequestTelemetry requestTelemetry = requestTelemetryCaptor.getValue();
        assertThat(requestTelemetry.getName(), is(equalTo(name)));
        assertThat(requestTelemetry.getDuration().getTotalMilliseconds(), is(equalTo(duration)));
        assertFalse(requestTelemetry.isSuccess());
    }

    @Test
    public void trackException_simple_shouldCallTrackException() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 1");

        // ACT
        classUnderTest.trackException(testException);

        // ASSERT
        verify(telemetryClient, times(1)).trackException(testException);
    }

    @Test
    public void trackException_complex_shouldUseExceptionTelemetry() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 2");

        // ACT
        classUnderTest.trackException(testException, null, null);

        // ASSERT
        ArgumentCaptor<ExceptionTelemetry> exceptionTelemetryCaptor = ArgumentCaptor.forClass(ExceptionTelemetry.class);
        verify(telemetryClient, times(1)).trackException(exceptionTelemetryCaptor.capture());
        ExceptionTelemetry exceptionTelemetry = exceptionTelemetryCaptor.getValue();
        assertThat(exceptionTelemetry.getException(), is(equalTo(testException)));
    }

    @Test
    public void trackException_complex_shouldUseExceptionTelemetry_withSeverityError() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 3");

        // ACT
        classUnderTest.trackException(testException, null, SeverityLevel.Error);

        // ASSERT
        ArgumentCaptor<ExceptionTelemetry> exceptionTelemetryCaptor = ArgumentCaptor.forClass(ExceptionTelemetry.class);
        verify(telemetryClient, times(1)).trackException(exceptionTelemetryCaptor.capture());
        ExceptionTelemetry exceptionTelemetry = exceptionTelemetryCaptor.getValue();
        assertThat(exceptionTelemetry.getSeverityLevel(), is(equalTo(SeverityLevel.Error)));
    }

    @Test
    public void trackException_complex_shouldUseExceptionTelemetry_withSeverityWarning() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 5");

        // ACT
        classUnderTest.trackException(testException, null, SeverityLevel.Warning);

        // ASSERT
        ArgumentCaptor<ExceptionTelemetry> exceptionTelemetryCaptor = ArgumentCaptor.forClass(ExceptionTelemetry.class);
        verify(telemetryClient, times(1)).trackException(exceptionTelemetryCaptor.capture());
        ExceptionTelemetry exceptionTelemetry = exceptionTelemetryCaptor.getValue();
        assertThat(exceptionTelemetry.getSeverityLevel(), is(equalTo(SeverityLevel.Warning)));
    }

    @Test
    public void trackException_complex_shouldUseExceptionTelemetry_withCustomProperties() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 6");
        Map<String, String> customProperties = new HashMap<>();
        customProperties.put("test1", "Test property 1");
        customProperties.put("test2", "Test property 2");

        // ACT
        classUnderTest.trackException(testException, customProperties, null);

        // ASSERT
        ArgumentCaptor<ExceptionTelemetry> exceptionTelemetryCaptor = ArgumentCaptor.forClass(ExceptionTelemetry.class);
        verify(telemetryClient, times(1)).trackException(exceptionTelemetryCaptor.capture());
        ExceptionTelemetry exceptionTelemetry = exceptionTelemetryCaptor.getValue();
        Map<String, String> exceptionTelemetryProperties =  exceptionTelemetry.getProperties();
        assertThat(exceptionTelemetryProperties.get("test1"), is(equalTo(customProperties.get("test1"))));
        assertThat(exceptionTelemetryProperties.get("test2"), is(equalTo(customProperties.get("test2"))));
    }

    @Test
    public void trackException_complex_shouldUseExceptionTelemetry_withCustomProperties_emptyDoesNotFail() {

        // ARRANGE
        IllegalStateException testException = new IllegalStateException("Test exception 7");

        // ACT
        classUnderTest.trackException(testException, new HashMap<>(), null);

        // ASSERT
        ArgumentCaptor<ExceptionTelemetry> exceptionTelemetryCaptor = ArgumentCaptor.forClass(ExceptionTelemetry.class);
        verify(telemetryClient, times(1)).trackException(exceptionTelemetryCaptor.capture());
        ExceptionTelemetry exceptionTelemetry = exceptionTelemetryCaptor.getValue();
        assertThat(exceptionTelemetry.getException(), is(equalTo(testException)));
    }

    @Test
    public void trackDependency_simple_shouldCallTrackDependency_successfulDependency() {

        // ARRANGE
        String dependencyName = "Test dependency name 1";
        String commandName = "Test command name 1";
        long duration = 123L;

        // ACT
        classUnderTest.trackDependency(dependencyName, commandName, duration, true);

        // ASSERT
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(telemetryClient, times(1)).trackDependency(eq(dependencyName), eq(commandName),
            durationCaptor.capture(), eq(true));
        assertThat(durationCaptor.getValue().getTotalMilliseconds(), is(equalTo(duration)));
    }

    @Test
    public void trackDependency_simple_shouldCallTrackDependency_failedDependency() {

        // ARRANGE
        String dependencyName = "Test dependency name 2";
        String commandName = "Test command name 2";
        long duration = 321L;

        // ACT
        classUnderTest.trackDependency(dependencyName, commandName, duration, false);

        // ASSERT
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(telemetryClient, times(1)).trackDependency(eq(dependencyName), eq(commandName),
            durationCaptor.capture(), eq(false));
        assertThat(durationCaptor.getValue().getTotalMilliseconds(), is(equalTo(duration)));
    }

    @Test
    public void trackCallBackDependency() {

        // ARRANGE
        CallbackType callbackType = CallbackType.ABOUT_TO_START;
        String url = "http://ccd-test-stub";
        String status = "200";
        java.time.Duration duration = java.time.Duration.ofMillis(1200);

        // ACT
        classUnderTest.trackCallbackEvent(callbackType, url, status, duration);

        // ASSERT
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(telemetryClient).trackEvent(eq(CALLBACK_EVENT_NAME), captor.capture(), eq(null));

        assertThat(captor.getValue().get(TYPE), is(equalTo(callbackType.getValue())));
        assertThat(captor.getValue().get(URI), is(equalTo(url)));
        assertThat(captor.getValue().get(STATUS), is(equalTo(status)));
        assertThat(captor.getValue().get(METHOD), is(equalTo("POST")));
        assertThat(captor.getValue().get(CALLBACK_DURATION), is(equalTo("1200 ms")));
    }

}
