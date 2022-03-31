package uk.gov.hmcts.ccd.wiremock.config;

import uk.gov.hmcts.ccd.wiremock.extensions.CustomisedResponseTransformer;
import uk.gov.hmcts.ccd.wiremock.extensions.DynamicOAuthJwkSetResponseTransformer;

import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockTestConfiguration {

    @Bean
    public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
        return config -> config.extensions(new CustomisedResponseTransformer(),
            new DynamicOAuthJwkSetResponseTransformer());
    }
}

