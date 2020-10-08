package uk.gov.hmcts.ccd.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryContext;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryThreadContext;

@Configuration
public class AppInsightsTracingConfiguration {

    @Bean
    // Custom TelemetryProcessor which tags the type of {@link RemoteDependencyTelemetry} as callback before publishing.
    public TelemetryProcessor callbackRecognitionProcessor() {
        return telemetry -> {
            if (telemetry instanceof RemoteDependencyTelemetry) {
                RemoteDependencyTelemetry dependency = (RemoteDependencyTelemetry) telemetry;
                if (dependency.getType().startsWith("Http")
                    && CallbackTelemetryThreadContext.getTelemetryContext() != null) {
                    CallbackTelemetryContext telemetryContext = CallbackTelemetryThreadContext.getTelemetryContext();
                    if (telemetryContext.getCallbackType() != null) {
                        dependency.getProperties().put("callback", "true");
                        dependency.getProperties().put("callbackType", telemetryContext.getCallbackType().getValue());
                    }
                    // clean up after retrieval
                    CallbackTelemetryThreadContext.remove();
                }
            }
            return true;
        };
    }
}
