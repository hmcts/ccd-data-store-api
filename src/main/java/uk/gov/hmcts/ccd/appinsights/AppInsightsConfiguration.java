package uk.gov.hmcts.ccd.appinsights;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Configuration
public class AppInsightsConfiguration {

    static final String CONFIG_TREE_CONNECTION_STRING_PROPERTY = "app-insights-connection-string";
    static final String CONNECTION_STRING_PROPERTY = "applicationinsights.connection-string";
    static final String LEGACY_INSTRUMENTATION_KEY_PROPERTY = "azure.application-insights.instrumentation-key";

    @Bean
    public TelemetryClient telemetryClient(Environment environment) {
        resolveConnectionString(environment).ifPresent(ConnectionString::configure);
        return new TelemetryClient();
    }

    static Optional<String> resolveConnectionString(Environment environment) {
        Binder binder = Binder.get(environment);
        String connectionString = binder.bind(CONFIG_TREE_CONNECTION_STRING_PROPERTY, String.class)
            .orElseGet(() -> binder.bind(CONNECTION_STRING_PROPERTY, String.class).orElse(null));
        if (StringUtils.hasText(connectionString)) {
            return Optional.of(connectionString);
        }

        // Preserve the environment variable used by the former 2.x Spring Boot starter.
        String instrumentationKey = binder.bind(LEGACY_INSTRUMENTATION_KEY_PROPERTY, String.class).orElse(null);
        if (StringUtils.hasText(instrumentationKey)) {
            return Optional.of("InstrumentationKey=" + instrumentationKey);
        }
        return Optional.empty();
    }
}
