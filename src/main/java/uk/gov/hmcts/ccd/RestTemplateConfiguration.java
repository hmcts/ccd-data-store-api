package uk.gov.hmcts.ccd;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
@Component
public class RestTemplateConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    @Value("${http.client.max.total}")
    private int maxTotalHttpClient;

    @Value("${http.client.seconds.idle.connection}")
    private int maxSecondsIdleConnection;

    @Value("${http.client.max.client_per_route}")
    private int maxClientPerRoute;

    @Value("${http.client.validate.after.inactivity}")
    private int validateAfterInactivity;

    @Value("${http.client.connection.timeout}")
    private int connectionTimeout;

    @Bean
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));
        return restTemplate;
    }

    public HttpClient getHttpClient() {
        return getHttpClient(connectionTimeout);
    }

    public HttpClient getHttpClient(final int timeout) {
        try (PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager()) {

            LOG.info("maxTotalHttpClient: {}", maxTotalHttpClient);
            LOG.info("maxSecondsIdleConnection: {}", maxSecondsIdleConnection);
            LOG.info("maxClientPerRoute: {}", maxClientPerRoute);
            LOG.info("validateAfterInactivity: {}", validateAfterInactivity);
            LOG.info("connectionTimeout: {}", timeout);

            cm.setMaxTotal(maxTotalHttpClient);
            cm.closeIdleConnections(maxSecondsIdleConnection, TimeUnit.SECONDS);
            cm.setDefaultMaxPerRoute(maxClientPerRoute);
            cm.setValidateAfterInactivity(validateAfterInactivity);

            final RequestConfig config =
                RequestConfig.custom()
                             .setConnectTimeout(timeout)
                             .setConnectionRequestTimeout(timeout)
                             .setSocketTimeout(timeout)
                             .build();

            return HttpClientBuilder.create()
                                    .useSystemProperties()
                                    .setDefaultRequestConfig(config)
                                    .setConnectionManager(cm)
                                    .build();
        }
    }
}
