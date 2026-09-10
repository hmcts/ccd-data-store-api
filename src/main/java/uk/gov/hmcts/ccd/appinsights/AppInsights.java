package uk.gov.hmcts.ccd.appinsights;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType;

import java.util.Map;

@Component
public class AppInsights {
    private static final String MODULE = "CASE_DATA";
    public static final String CASE_DEFINITION = "CASE_DEFINITION";
    public static final String DOC_MANAGEMENT = "DOCUMENT_MANAGEMENT";
    public static final String DRAFT_STORE = "DRAFT_STORE";

    public static final String TYPE = "Callback type";
    public static final String CALLBACK_DURATION = "Callback duration";
    public static final String METHOD = "Method";
    public static final String URI = "URI";
    public static final String STATUS = "Http Status";
    public static final String CALLBACK_EVENT_NAME = "CALLBACK";
    public static final String CALLBACK_DEPENDENCY_PROPERTY = "callback";
    public static final String CALLBACK_TYPE_PROPERTY = "callbackType";
    public static final String MANUAL_CALLBACK_DEPENDENCY_PROPERTY = "manualCallbackDependency";

    private static final String HTTP_DEPENDENCY_TYPE = "Http";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String UNKNOWN = "unknown";

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

    public void trackTrace(String message, Map<String, String> customProperties, SeverityLevel severityLevel) {
        telemetry.trackTrace(message, severityLevel, customProperties);
    }

    public void trackCallbackEvent(
        CallbackType callbackType, String url, String httpStatus, java.time.Duration duration) {
        String safeUrl = nullSafe(url);
        Map<String, String> properties = Map.of(
            TYPE, callbackType != null ? callbackType.getValue() : "null",
            CALLBACK_DURATION, String.valueOf(duration.toMillis()) + " ms",
            METHOD, HTTP_METHOD_POST,
            URI, safeUrl,
            STATUS, httpStatus
        );
        telemetry.trackEvent(CALLBACK_EVENT_NAME, properties, null);
    }

    // Azure Portal callback dashboards filter dependency telemetry on customDimensions.callback == "true".
    // manualCallbackDependency identifies this replacement telemetry so generic dependency KQL can exclude it.
    public void trackCallbackDependency(
        CallbackType callbackType, String url, String httpStatus, java.time.Duration duration) {

        RemoteDependencyTelemetry dependencyTelemetry = new RemoteDependencyTelemetry();
        String safeUrl = nullSafe(url);
        String target = dependencyTarget(safeUrl);

        dependencyTelemetry.setName(HTTP_METHOD_POST + " " + target);
        dependencyTelemetry.setCommandName(safeUrl);
        dependencyTelemetry.setType(HTTP_DEPENDENCY_TYPE);
        dependencyTelemetry.setTarget(target);
        dependencyTelemetry.setResultCode(httpStatus);
        dependencyTelemetry.setDuration(new Duration(duration.toMillis()));
        dependencyTelemetry.setSuccess(isSuccessfulHttpStatus(httpStatus));
        dependencyTelemetry.getProperties().put(CALLBACK_DEPENDENCY_PROPERTY, "true");
        dependencyTelemetry.getProperties().put(MANUAL_CALLBACK_DEPENDENCY_PROPERTY, "true");
        if (callbackType != null) {
            dependencyTelemetry.getProperties().put(CALLBACK_TYPE_PROPERTY, callbackType.getValue());
        }

        telemetry.trackDependency(dependencyTelemetry);
    }

    private String dependencyTarget(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null ? host : url;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private boolean isSuccessfulHttpStatus(String httpStatus) {
        try {
            int status = Integer.parseInt(httpStatus);
            return status >= 200 && status < 400;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : UNKNOWN;
    }
}
