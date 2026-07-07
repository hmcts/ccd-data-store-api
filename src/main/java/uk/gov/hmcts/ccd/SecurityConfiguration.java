package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.ccd.security.OidcIssuerConfiguration;
import uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter;
import uk.gov.hmcts.ccd.security.filters.SecurityLoggingFilter;
import uk.gov.hmcts.ccd.security.filters.V1EndpointsPathParamSecurityFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final String issuerUri;
    private final String primaryIssuer;
    private final String configuredAllowedIssuers;
    private final ServiceAuthFilter serviceAuthFilter;
    private final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter;
    private final SecurityLoggingFilter securityLoggingFilter;
    private final ExceptionHandlingFilter exceptionHandlingFilter;
    private final CustomHeadersFilter customHeadersFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String[] AUTH_WHITELIST = {
        "/v3/api-docs",
        "/v2/**",
        "/health/liveness",
        "/health/readiness",
        "/health",
        "/",
        "/status/health",
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/v2/api-docs",
        "/v2/api-docs/**",
        "/testing-support/cleanup-case-type/**"
    };

    @Inject
    public SecurityConfiguration(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                 final ServiceAuthFilter serviceAuthFilter,
                                 final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                 final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                 final SecurityUtils securityUtils,
                                 final ApplicationParams applicationParams,
                                 @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuerUri,
                                 @Value("${oidc.issuer}") String primaryIssuer,
                                 @Value("${oidc.allowed-issuers:}") String configuredAllowedIssuers,
                                 @Value("${security.logging.filter.path.regex}") String loggingFilterPathRegex) {
        this.issuerUri = issuerUri;
        this.primaryIssuer = primaryIssuer;
        this.configuredAllowedIssuers = configuredAllowedIssuers;
        this.customHeadersFilter = new CustomHeadersFilter(applicationParams);
        this.v1EndpointsPathParamSecurityFilter = new V1EndpointsPathParamSecurityFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils);
        this.securityLoggingFilter = new SecurityLoggingFilter(securityUtils, loggingFilterPathRegex);
        this.serviceAuthFilter = serviceAuthFilter;
        this.exceptionHandlingFilter = new ExceptionHandlingFilter();
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(AUTH_WHITELIST);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(customHeadersFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(exceptionHandlingFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(securityLoggingFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(v1EndpointsPathParamSecurityFilter, SecurityLoggingFilter.class)
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .csrf(csrf -> csrf.disable()) // NOSONAR - CSRF is disabled purposely
            .formLogin(fl -> fl.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/error")
                .permitAll()
                .anyRequest()
                .authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)JwtDecoders.fromOidcIssuerLocation(issuerUri);

        // See docs/security/jwt-issuer-validation.md for discovery and issuer enforcement.
        jwtDecoder.setJwtValidator(jwtValidator(primaryIssuer, configuredAllowedIssuers));
        return jwtDecoder;
    }

    static OAuth2TokenValidator<Jwt> jwtValidator(String primaryIssuer, String configuredAllowedIssuers) {
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        Set<String> allowedIssuers = OidcIssuerConfiguration.allowedIssuers(primaryIssuer, configuredAllowedIssuers);
        OAuth2TokenValidator<Jwt> withIssuer = new JwtClaimValidator<>(
            "iss",
            issuer -> issuer != null && allowedIssuers.contains(issuer.toString())
        );
        return new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
    }

}
