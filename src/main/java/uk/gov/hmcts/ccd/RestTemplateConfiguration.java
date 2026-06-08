package uk.gov.hmcts.ccd;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
class RestTemplateConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    private final List<PoolingHttpClientConnectionManager> connectionManagers = new ArrayList<>();

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

    @Value("${http.client.read.timeout}")
    private int readTimeout;

    @Value("${http.client.connection.drafts.timeout}")
    private int draftsConnectionTimeout;

    @Value("${http.client.connection.drafts.create.timeout}")
    private int draftsCreateConnectionTimeout;

    @Value("${http.client.connection.definition-store.timeout}")
    private int definitionStoreConnectionTimeout;

    @Bean(name = "definitionStoreRestTemplate")
    public RestTemplate definitionStoreRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(
            new HttpComponentsClientHttpRequestFactory(getHttpClient(definitionStoreConnectionTimeout)));
        LOG.info("definitionStoreConnectionTimeout: {}", definitionStoreConnectionTimeout);
        return restTemplate;
    }

    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(getHttpClient(connectionTimeout, readTimeout));
        LOG.info("connectionTimeout: {}, readTimeout: {}", connectionTimeout, readTimeout);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    @Bean(name = "documentRestTemplate")
    public RestTemplate documentRestTemplate() {

        final RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(getHttpClient());
        LOG.info("connectionTimeout: {}, readTimeout: {}", connectionTimeout, readTimeout);
        restTemplate.setRequestFactory(requestFactory);

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        //Add the Jackson Message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // Note: here we are making this converter to process any kind of response,
        // not only application/*json, which is the default behaviour
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }

    @Bean(name = "createDraftRestTemplate")
    public RestTemplate createDraftsRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(
            new HttpComponentsClientHttpRequestFactory(getHttpClient(draftsCreateConnectionTimeout)));
        return restTemplate;
    }

    @Bean(name = "draftsRestTemplate")
    public RestTemplate draftsRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(
            new HttpComponentsClientHttpRequestFactory(getHttpClient(draftsConnectionTimeout)));
        return restTemplate;
    }

    @PreDestroy
    void close() {
        LOG.info("PreDestroy called");
        connectionManagers.forEach(cm -> {
            LOG.info("closing connection manager");
            cm.close();
        });
    }

    private HttpClient getHttpClient() {
        return getHttpClient(connectionTimeout, readTimeout);
    }

    private HttpClient getHttpClient(final int connectTimeout) {
        return getHttpClient(connectTimeout, readTimeout);
    }

    private HttpClient getHttpClient(final int connectTimeout, final int readTimeout) {
        PoolingHttpClientConnectionManager cm = buildConnectionManager(connectTimeout, readTimeout);
        connectionManagers.add(cm);

        final RequestConfig config =
            RequestConfig.custom()
                         .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout))
                         .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                         .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                         .build();

        return HttpClientBuilder.create()
                                .useSystemProperties()
                                .setDefaultRequestConfig(config)
                                .setConnectionManager(cm)
                                .build();
    }

    private PoolingHttpClientConnectionManager buildConnectionManager(final int connectTimeout,
                                                                      final int readTimeout) {
        LOG.info("maxTotalHttpClient: {}", maxTotalHttpClient);
        LOG.info("maxSecondsIdleConnection: {}", maxSecondsIdleConnection);
        LOG.info("maxClientPerRoute: {}", maxClientPerRoute);
        LOG.info("validateAfterInactivity: {}", validateAfterInactivity);
        LOG.info("connectionTimeout: {}, readTimeout: {} ", connectTimeout, readTimeout);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxTotalHttpClient);
        cm.closeIdle(TimeValue.ofSeconds(maxSecondsIdleConnection));
        cm.setDefaultMaxPerRoute(maxClientPerRoute);
        Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver = new Resolver<HttpRoute,ConnectionConfig>() {
            @Override
            public ConnectionConfig resolve(HttpRoute route) {
                return ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                        .setSocketTimeout(Timeout.ofMilliseconds(readTimeout))
                        .setValidateAfterInactivity(TimeValue.ofMilliseconds(validateAfterInactivity))
                        .build();
            }
        };
        cm.setConnectionConfigResolver(connectionConfigResolver);
        Resolver<HttpRoute, SocketConfig> socketConfigResolver = new Resolver<HttpRoute,SocketConfig>() {
            @Override
            public SocketConfig resolve(HttpRoute route) {
                return SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                        .build();
            }
        };
        cm.setSocketConfigResolver(socketConfigResolver);

        return cm;
    }
}
