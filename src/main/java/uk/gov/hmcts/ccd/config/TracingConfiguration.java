package uk.gov.hmcts.ccd.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    public TelemetryProcessor healthRecognitionProcessor() {
        return telemetry -> {
            if (telemetry instanceof RemoteDependencyTelemetry) {
                RemoteDependencyTelemetry dependencyTel = (RemoteDependencyTelemetry) telemetry;
                if (dependencyTel.getType().startsWith("Http") && dependencyTel.getName().startsWith("POST")) {
                    dependencyTel.getProperties().put("callback", "true");
                    dependencyTel.setType("callback");
                }
            }
            return true;
        };
    }
}
