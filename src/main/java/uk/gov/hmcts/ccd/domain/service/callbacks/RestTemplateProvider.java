package uk.gov.hmcts.ccd.domain.service.callbacks;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service
public class RestTemplateProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateProvider.class);
    private final HttpClient httpClient;

    @Autowired
    public RestTemplateProvider(@Qualifier("httpClient") final HttpClient httpClient) {
        this.httpClient = httpClient;
    }


    public RestTemplate provide(Integer timeoutInSeconds) {
        LOG.info("Building rest template with read timeoutInSeconds interval {}", timeoutInSeconds);
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(secondsToMilliseconds(timeoutInSeconds));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    private int secondsToMilliseconds(final Integer timeout) {
        return (int) TimeUnit.SECONDS.toMillis(timeout);
    }
}
