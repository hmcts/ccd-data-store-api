package uk.gov.hmcts.ccd;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppInsights {
    private static final String MODULE = "CASE_DATA";
    public static final String CASE_DEFINITION = "CASE_DEFINITION";
    public static final String DOC_MANAGEMENT = "DOCUMENT_MANAGEMENT";
    public static final String DRAFT_STORE = "DRAFT_STORE";
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

    public void trackDependency(String dependencyName, String commandName, long duration, boolean success) {
        telemetry.trackDependency(dependencyName, commandName, new Duration(duration), success);
    }
}
