package uk.gov.hmcts.ccd.appinsights;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.support.SystemEnvironmentPropertySourceEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppInsightsConfigurationTest {

    @Test
    void shouldResolveInstrumentationKeyFromExistingAzureEnvironmentVariable() {
        StandardEnvironment environment = environmentWith(
            "AZURE_APPLICATIONINSIGHTS_INSTRUMENTATIONKEY", "test-instrumentation-key"
        );

        assertThat(AppInsightsConfiguration.resolveConnectionString(environment))
            .contains("InstrumentationKey=test-instrumentation-key");
    }

    @Test
    void shouldResolveOfficialConnectionStringEnvironmentVariable() {
        StandardEnvironment environment = environmentWith(
            "APPLICATIONINSIGHTS_CONNECTION_STRING",
            "InstrumentationKey=00000000-0000-0000-0000-000000000000"
        );

        assertThat(AppInsightsConfiguration.resolveConnectionString(environment))
            .contains("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    }

    @Test
    void shouldPreferMountedConfigTreeConnectionString() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app-insights-connection-string", "InstrumentationKey=config-tree")
            .withProperty("applicationinsights.connection-string", "InstrumentationKey=environment")
            .withProperty("azure.application-insights.instrumentation-key", "legacy-key");

        assertThat(AppInsightsConfiguration.resolveConnectionString(environment))
            .contains("InstrumentationKey=config-tree");
    }

    @Test
    void shouldLeaveConnectionStringUnconfiguredWhenNoValueIsAvailable() {
        assertThat(AppInsightsConfiguration.resolveConnectionString(new MockEnvironment())).isEmpty();
    }

    private static StandardEnvironment environmentWith(String... propertyPairs) {
        Map<String, Object> properties = new java.util.HashMap<>();
        for (int index = 0; index < propertyPairs.length; index += 2) {
            properties.put(propertyPairs[index], propertyPairs[index + 1]);
        }

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                properties
            )
        );
        new SystemEnvironmentPropertySourceEnvironmentPostProcessor()
            .postProcessEnvironment(environment, new SpringApplication());
        return environment;
    }
}
