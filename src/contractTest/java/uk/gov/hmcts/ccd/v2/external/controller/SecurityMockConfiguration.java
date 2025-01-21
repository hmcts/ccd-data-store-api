package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.ccd.security.filters.SecurityLoggingFilter;
import uk.gov.hmcts.ccd.security.filters.V1EndpointsPathParamSecurityFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;


@Profile("SECURITY_MOCK")
@Configuration
@EnableWebSecurity
public class SecurityMockConfiguration {


    private final ServiceAuthFilter serviceAuthFilter;
    private final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter;
    private final SecurityLoggingFilter securityLoggingFilter;
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Inject
    public SecurityMockConfiguration(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                     final ServiceAuthFilter serviceAuthFilter,
                                     final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                     final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                     final SecurityUtils securityUtils,
                                     @Value("${security.logging.filter.path.regex}") String loggingFilterPathRegex) {
        this.v1EndpointsPathParamSecurityFilter = new V1EndpointsPathParamSecurityFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils);
        this.securityLoggingFilter = new SecurityLoggingFilter(securityUtils, loggingFilterPathRegex);
        this.serviceAuthFilter = serviceAuthFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/case-types/**",
            "/caseworkers/**",
            "/citizens/**",
            "/searchCases/**",
            "/cases/**",
            "/");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(ahr ->  ahr.requestMatchers(
            "/case-types/**",
            "/caseworkers/**",
            "/citizens/**",
            "/searchCases/**",
            "/cases/**"
        ).permitAll());
        return http.build();
    }
}
