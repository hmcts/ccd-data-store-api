package uk.gov.hmcts.ccd;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

class RestTemplateConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(RestTemplateConfiguration.class)
        .withPropertyValues(
            "http.client.max.total=50",
            "http.client.seconds.idle.connection=30",
            "http.client.max.client_per_route=10",
            "http.client.validate.after.inactivity=1000",
            "http.client.connection.timeout=2000",
            "http.client.read.timeout=3000",
            "http.client.connection.drafts.timeout=2500",
            "http.client.connection.drafts.create.timeout=2600",
            "http.client.connection.definition-store.timeout=2700"
        );

    @Test
    void shouldCreateAndShutdownConnectionManagerPerRestTemplate() {
        AtomicReference<List<PoolingHttpClientConnectionManager>> constructedRef = new AtomicReference<>();

        try (MockedConstruction<PoolingHttpClientConnectionManager> connectionManagers =
                 mockConstruction(PoolingHttpClientConnectionManager.class, withSettings())) {

            contextRunner.run(context -> {
                // Force creation of all RestTemplate beans to register managers.
                context.getBean("definitionStoreRestTemplate");
                context.getBean("restTemplate");
                context.getBean("documentRestTemplate");
                context.getBean("createDraftRestTemplate");
                context.getBean("draftsRestTemplate");

                constructedRef.set(connectionManagers.constructed());
                assertThat(constructedRef.get()).hasSize(5);
            });

            // After the run block, the context is closed and @PreDestroy should have fired.
            List<PoolingHttpClientConnectionManager> createdManagers = constructedRef.get();
            assertThat(createdManagers).hasSize(5);
            createdManagers.forEach(cm -> verify(cm).close());
        }
    }

    @Test
    void shouldCloseManagersAfterRestTemplateUsage() {
        AtomicReference<List<PoolingHttpClientConnectionManager>> constructedRef = new AtomicReference<>();

        try (MockedConstruction<PoolingHttpClientConnectionManager> connectionManagers =
                 mockConstruction(PoolingHttpClientConnectionManager.class, withSettings())) {

            contextRunner.run(context -> {
                // Fetch beans and touch their factories to force client construction.
                context.getBean("definitionStoreRestTemplate", org.springframework.web.client.RestTemplate.class)
                    .getRequestFactory();
                context.getBean("restTemplate", org.springframework.web.client.RestTemplate.class)
                    .getRequestFactory();
                context.getBean("documentRestTemplate", org.springframework.web.client.RestTemplate.class)
                    .getRequestFactory();
                context.getBean("createDraftRestTemplate", org.springframework.web.client.RestTemplate.class)
                    .getRequestFactory();
                context.getBean("draftsRestTemplate", org.springframework.web.client.RestTemplate.class)
                    .getRequestFactory();

                constructedRef.set(connectionManagers.constructed());
                assertThat(constructedRef.get()).hasSize(5);
            });

            // Context closed; @PreDestroy should close all managers.
            List<PoolingHttpClientConnectionManager> createdManagers = constructedRef.get();
            assertThat(createdManagers).hasSize(5);
            createdManagers.forEach(cm -> verify(cm).close());
        }
    }
}
