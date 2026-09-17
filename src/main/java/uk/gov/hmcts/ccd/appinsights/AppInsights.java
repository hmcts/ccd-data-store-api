package uk.gov.hmcts.ccd.appinsights;

import com.google.common.collect.ImmutableMap;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType;

import java.util.Map;

@Component
public class AppInsights {
    private static final String MODULE = "CASE_DATA";

    public static final String TYPE = "Callback type";
    public static final String CALLBACK_DURATION = "Callback duration";
    public static final String METHOD = "Method";
    public static final String URI = "URI";
    public static final String STATUS = "Http Status";
    public static final String CALLBACK_EVENT_NAME = "CALLBACK";

    private final TelemetryClient telemetry;

    @Autowired
    public AppInsights(TelemetryClient telemetry) {
        this.telemetry = telemetry;
    }

    public void trackRequest(String name, long duration, boolean success) {
        RequestTelemetry rt = new RequestTelemetry();
        rt.setSource(MODULE);
        rt.setName(name);
        rt.setSuccess(success);

        rt.setDuration(new Duration(duration));
        telemetry.trackRequest(rt);
    }

    public void trackException(Exception e) {
        telemetry.trackException(e);
    }

    /**
     * Sends an exception record to Application Insights. Appears in "exceptions" in Analytics and Search.
     * @param exception The exception to log information about.
     * @param customProperties Named string values you can use to search and classify trace messages.
     * @param severityLevel Sets the SeverityLevel property
     */
    public void trackException(Exception exception, Map<String, String> customProperties, SeverityLevel severityLevel) {
        ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(exception);

        if (severityLevel != null) {
            exceptionTelemetry.setSeverityLevel(severityLevel);
        }

        if (customProperties != null && !customProperties.isEmpty()) {
            exceptionTelemetry.getContext().getProperties().putAll(customProperties);
        }

        telemetry.trackException(exceptionTelemetry);
    }

    public void trackDependency(String dependencyName, String commandName, long duration, boolean success) {
        telemetry.trackDependency(dependencyName, commandName, new Duration(duration), success);
    }

    public void trackEvent(String name, Map<String, String> properties) {
        telemetry.trackEvent(name, properties, null);
    }

    /**
     * Sends a custom event with numeric measurements alongside the string properties. The measurements land in
     * {@code customMeasurements} rather than {@code customDimensions}, so they can be aggregated in a KQL query
     * without a {@code todouble} conversion of a string.
     *
     * @param name       The event name.
     * @param properties Named string values, searchable as {@code customDimensions}.
     * @param metrics    Named numeric values, aggregatable as {@code customMeasurements}.
     */
    public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        telemetry.trackEvent(name, properties, metrics);
    }

    public void trackTrace(String message, Map<String, String> customProperties, SeverityLevel severityLevel) {
        telemetry.trackTrace(message, severityLevel, customProperties);
    }

    public void trackCallbackEvent(
        CallbackType callbackType, String url, String httpStatus, java.time.Duration duration) {
        Map<String, String> properties = ImmutableMap.of(
            TYPE, callbackType.getValue(),
            CALLBACK_DURATION, String.valueOf(duration.toMillis()) + " ms",
            METHOD, "POST",
            URI, url,
            STATUS, httpStatus
        );
        telemetry.trackEvent(CALLBACK_EVENT_NAME, properties, null);
    }
}
