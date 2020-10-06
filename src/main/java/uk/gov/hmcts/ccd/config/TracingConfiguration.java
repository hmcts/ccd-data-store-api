package uk.gov.hmcts.ccd.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    // Custom TelemetryProcessor which tags the type of {@link RemoteDependencyTelemetry} as callback before publishing.
    public TelemetryProcessor callbackRecognitionProcessor() {
        return telemetry -> {
            if (telemetry instanceof RemoteDependencyTelemetry) {
                RemoteDependencyTelemetry dependency = (RemoteDependencyTelemetry) telemetry;
                if (dependency.getType().startsWith("Http") && dependency.getName().startsWith("POST")
                    && !dependency.getName().contains("/lease")) { // ignore S2S path
                    dependency.getProperties().put("callback", "true");
                }
            }
            return true;
        };
    }

}
