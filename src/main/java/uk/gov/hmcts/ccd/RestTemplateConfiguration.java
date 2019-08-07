package uk.gov.hmcts.ccd;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
class RestTemplateConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private PoolingHttpClientConnectionManager draftsCm = new PoolingHttpClientConnectionManager();
    private PoolingHttpClientConnectionManager cbCm = new PoolingHttpClientConnectionManager();

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

    @Value("${http.client.connection.callbacks.timeout}")
    private int callbackConnectionTimeout;

    @Value("${http.client.read.callbacks.timeout}")
    private int callbackReadTimeout;

    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(getHttpClient());
        requestFactory.setReadTimeout(readTimeout);
        LOG.info("readTimeout: {}", readTimeout);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    @Bean(name = "createDraftRestTemplate")
    public RestTemplate createDraftsRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient(draftsCm, draftsCreateConnectionTimeout)));
        return restTemplate;
    }

    @Bean(name = "draftsRestTemplate")
    public RestTemplate draftsRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient(draftsCm, draftsConnectionTimeout)));
        return restTemplate;
    }

    @Bean(name = "callbackRestTemplate")
    public RestTemplate callbackRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(getHttpClient(cbCm, callbackConnectionTimeout));
        requestFactory.setReadTimeout(callbackReadTimeout);
        LOG.info("callbackReadTimeout: {}", callbackReadTimeout);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    @Bean(name = "callbacksExecutor")
    public ExecutorService callbacksExecutor() {
        return Executors.newFixedThreadPool(maxTotalHttpClient);
    }

    @PreDestroy
    void close() {
        LOG.info("PreDestory called");
        if (null != cm) {
            LOG.info("closing connection manager");
            cm.close();
        }
        if (null != draftsCm) {
            LOG.info("closing connection manager");
            cm.close();
        }
        if (null != cbCm) {
            LOG.info("closing connection manager");
            cm.close();
        }
    }

    private HttpClient getHttpClient() {
        return getHttpClient(cm, connectionTimeout);
    }

    private HttpClient getHttpClient(PoolingHttpClientConnectionManager cm, final int timeout) {
        cm = new PoolingHttpClientConnectionManager();

        LOG.info("maxTotalHttpClient: {}", maxTotalHttpClient);
        LOG.info("maxSecondsIdleConnection: {}", maxSecondsIdleConnection);
        LOG.info("maxClientPerRoute: {}", maxClientPerRoute);
        LOG.info("validateAfterInactivity: {}", validateAfterInactivity);
        LOG.info("connectionTimeout: {}", timeout);

        cm.setMaxTotal(maxTotalHttpClient);
        cm.closeIdleConnections(maxSecondsIdleConnection, TimeUnit.SECONDS);
        cm.setDefaultMaxPerRoute(maxClientPerRoute);
        cm.setValidateAfterInactivity(validateAfterInactivity);
        final RequestConfig
            config =
            RequestConfig.custom()
                         .setConnectTimeout(timeout)
                         .setConnectionRequestTimeout(timeout)
                         .setSocketTimeout(timeout)
                         .build();

        return HttpClientBuilder.create()
                                .useSystemProperties()
                                .setDefaultRequestConfig(config)
                                .setConnectionManager(cm)
                                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, false))
                                .build();
    }
}
