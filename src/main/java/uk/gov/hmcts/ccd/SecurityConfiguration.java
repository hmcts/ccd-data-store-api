package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.ccd.security.filters.ExceptionHandlingFilter;
import uk.gov.hmcts.ccd.security.filters.SecurityLoggingFilter;
import uk.gov.hmcts.ccd.security.filters.V1EndpointsPathParamSecurityFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final ServiceAuthFilter serviceAuthFilter;
    private final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter;
    private final SecurityLoggingFilter securityLoggingFilter;
    private final ExceptionHandlingFilter exceptionHandlingFilter;
    private final CustomHeadersFilter customHeadersFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ApplicationParams applicationParams;

    private static final String[] AUTH_WHITELIST = {
        "/v2/**",
        "/health/liveness",
        "/health/readiness",
        "/health",
        "/loggers/**",
        "/",
        "/status/health",
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/testing-support/cleanup-case-type/**"
    };

    @Inject
    public SecurityConfiguration(
        final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
        final ServiceAuthFilter serviceAuthFilter,
        final Function<HttpServletRequest, Optional<String>> userIdExtractor,
        final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
        final SecurityUtils securityUtils,
        final ApplicationParams applicationParams,
        @Value("${security.logging.filter.path.regex}") String loggingFilterPathRegex
    ) {
        this.applicationParams = applicationParams;
        this.customHeadersFilter = new CustomHeadersFilter(applicationParams);
        this.v1EndpointsPathParamSecurityFilter = new V1EndpointsPathParamSecurityFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils);
        this.securityLoggingFilter = new SecurityLoggingFilter(securityUtils, loggingFilterPathRegex);
        this.serviceAuthFilter = serviceAuthFilter;
        this.exceptionHandlingFilter = new ExceptionHandlingFilter();
        this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
        this.jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(customHeadersFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(exceptionHandlingFilter, BearerTokenAuthenticationFilter.class)
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(securityLoggingFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(v1EndpointsPathParamSecurityFilter, SecurityLoggingFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(Arrays.stream(AUTH_WHITELIST)
                    .map(AntPathRequestMatcher::new)
                    .toArray(AntPathRequestMatcher[]::new)
                ).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .oauth2Client(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);
        // If issuer override becomes valid later, add it like below:
        // OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }
}
