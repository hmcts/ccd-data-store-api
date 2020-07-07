package uk.gov.hmcts.ccd;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.ccd.security.filters.V1EndpointsPathParamSecurityFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final ServiceAuthFilter serviceAuthFilter;
    private final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter;
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String[] AUTH_WHITELIST = {
        "/swagger-ui.html",
        "/webjars/springfox-swagger-ui/**",
        "/swagger-resources/**",
        "/v2/**",
        "/health",
        "/health/liveness",
        "/status/health",
        "/loggers/**",
        "/"
    };

    @Inject
    public SecurityConfiguration(final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter,
                                 final ServiceAuthFilter serviceAuthFilter,
                                 final Function<HttpServletRequest, Optional<String>> userIdExtractor,
                                 final Function<HttpServletRequest, Collection<String>> authorizedRolesExtractor,
                                 final SecurityUtils securityUtils) {
        this.v1EndpointsPathParamSecurityFilter = new V1EndpointsPathParamSecurityFilter(
            userIdExtractor, authorizedRolesExtractor, securityUtils);
        this.serviceAuthFilter = serviceAuthFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(AUTH_WHITELIST);
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(v1EndpointsPathParamSecurityFilter, BearerTokenAuthenticationFilter.class)
            .sessionManagement().sessionCreationPolicy(STATELESS).and()
            .csrf().disable()
            .formLogin().disable()
            .logout().disable()
            .authorizeRequests()
            // To preserve v1EndpointsPathParamSecurityFilter 403 response
            .antMatchers("/error").permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(jwtAuthenticationConverter)
            .and()
            .and()
            .oauth2Client();

    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)JwtDecoders.fromOidcIssuerLocation(issuerUri);

        // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
        // FIXME : enable `withIssuer` once idam migration done RDM-8094
        // OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

}
