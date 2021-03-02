package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.context.ContextCleanupListener;

@Configuration
public class TestIdamConfiguration extends ContextCleanupListener {

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Bean
    // Overriding as OAuth2ClientRegistrationRepositoryConfiguration loading before wire-mock mappings
    // for /o/.well-known/openid-configuration
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(clientRegistration());
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("oidc")
            .redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("read:user")
            .authorizationUri("http://idam/o/authorize")
            .tokenUri("http://idam/o/access_token")
            .userInfoUri("http://idam/o/userinfo")
            .userNameAttributeName("id")
            .clientName("Client Name")
            .clientId("client-id")
            .clientSecret("client-secret")
            .build();
    }
}
