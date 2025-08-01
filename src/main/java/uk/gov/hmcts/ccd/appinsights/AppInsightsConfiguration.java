package uk.gov.hmcts.ccd.appinsights;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppInsightsConfiguration {

    @Value("${azure.application-insights.instrumentation-key:#{null}}")
    private String instrumentationKey;
    
    @Value("${app.insights.log.request.enabled:true}")
    private boolean logRequestsEnabled;
    
    @Value("${app.insights.log.trace.enabled:true}")
    private boolean logTraceEnabled;
    
    @Bean
    public TelemetryClient telemetryClient() {
        TelemetryClient client = new TelemetryClient();
        
        // Only enable App Insights if we have an instrumentation key
        if (instrumentationKey != null && !instrumentationKey.isEmpty()) {
            TelemetryConfiguration configuration = TelemetryConfiguration.getActive();
            configuration.setTrackingIsDisabled(!logRequestsEnabled);
            
            // Set trace telemetry configuration
            // Since we can't directly disable trace telemetry, we'll set it in properties
            // that our cost optimization processor can use
            if (!logTraceEnabled) {
                client.getContext().getProperties().put("DisableTraceTelemetry", "true");
            }
        }
        
        return client;
    }
}