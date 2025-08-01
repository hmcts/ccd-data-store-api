package uk.gov.hmcts.ccd.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryContext;
import uk.gov.hmcts.ccd.appinsights.CallbackTelemetryThreadContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Configuration
public class AppInsightsTracingConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AppInsightsTracingConfiguration.class);
    
    // List of paths that are high volume but low value for monitoring
    private static final List<String> LOW_VALUE_PATHS = Arrays.asList(
        "/health", 
        "/health/liveness",
        "/health/readiness",
        "/actuator",
        "/v2/api-docs", 
        "/swagger-resources",
        "/favicon.ico"
    );

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
    
    @Bean
    // Cost optimization processor to filter out unnecessary telemetry
    public TelemetryProcessor costOptimizationProcessor() {
        return telemetry -> {
            // Filter out health endpoints
            if (telemetry instanceof RequestTelemetry) {
                RequestTelemetry request = (RequestTelemetry) telemetry;
                
                try {
                    URL url = request.getUrl();
                    if (url != null) {
                        String urlString = url.toString().toLowerCase();
                        
                        // Filter out common health check and documentation endpoints
                        for (String path : LOW_VALUE_PATHS) {
                            if (urlString.contains(path)) {
                                LOG.debug("Filtering out telemetry for low-value path: {}", urlString);
                                return false;
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    LOG.debug("Could not parse URL from request telemetry: {}", e.getMessage());
                }
                
                // Use the properties to determine HTTP method as getHttpMethod is deprecated
                if (request.getProperties().containsKey("HttpMethod") 
                        && "OPTIONS".equals(request.getProperties().get("HttpMethod"))) {
                    LOG.debug("Filtering out OPTIONS request");
                    return false;
                }
            }
            
            // Filter out specific dependency calls that are high volume
            if (telemetry instanceof RemoteDependencyTelemetry) {
                RemoteDependencyTelemetry dependency = (RemoteDependencyTelemetry) telemetry;
                String target = dependency.getTarget();
                String name = dependency.getName();
                
                // Filter out database health checks and other frequent low-value dependencies
                if ((target != null && target.contains("postgresql")) 
                        && (name != null && name.startsWith("SELECT 1"))) {
                    LOG.debug("Filtering out database health check telemetry");
                    return false;
                }
            }
            
            // Only track critical and error exceptions by default
            if (telemetry instanceof ExceptionTelemetry) {
                ExceptionTelemetry exception = (ExceptionTelemetry) telemetry;
                
                // Filter out expected exceptions that are properly handled
                if (exception.getException() != null) {
                    String exceptionName = exception.getException().getClass().getName();
                    if (exceptionName.contains("ResourceNotFoundException") 
                            || exceptionName.contains("AccessDeniedException")) {
                        LOG.debug("Filtering out expected exception: {}", exceptionName);
                        return false;
                    }
                }
            }
            
            // Check if trace telemetry should be disabled based on configuration
            if (telemetry.getContext() != null && 
                telemetry.getContext().getProperties().containsKey("DisableTraceTelemetry") &&
                "true".equals(telemetry.getContext().getProperties().get("DisableTraceTelemetry"))) {
                if (telemetry.getClass().getName().contains("TraceTelemetry")) {
                    LOG.debug("Filtering out trace telemetry based on configuration");
                    return false;
                }
            }
            
            return true;
        };
    }
}
