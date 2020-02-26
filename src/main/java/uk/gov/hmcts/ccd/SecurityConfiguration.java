package uk.gov.hmcts.ccd;

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
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import uk.gov.hmcts.ccd.security.JwtAuthorityExtractor;
import uk.gov.hmcts.ccd.security.filters.V1EndpointsPathParamSecurityFilter;
import uk.gov.hmcts.ccd.security.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final ServiceAuthFilter serviceAuthFilter;
    private final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter;
    private final JwtAuthorityExtractor jwtAuthorityExtractor;

    @Inject
    public SecurityConfiguration(final JwtAuthorityExtractor jwtAuthorityExtractor,
                                 final AuthTokenValidator authTokenValidator,
                                 final V1EndpointsPathParamSecurityFilter v1EndpointsPathParamSecurityFilter,
                                 @Value("#{'${casedatastore.authorised.services}'.split(',')}")
                                         List<String> authorisedServices) {
        this.v1EndpointsPathParamSecurityFilter = v1EndpointsPathParamSecurityFilter;
        this.serviceAuthFilter = new ServiceAuthFilter(authTokenValidator, authorisedServices);
        this.jwtAuthorityExtractor = jwtAuthorityExtractor;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/swagger-ui.html",
            "/webjars/springfox-swagger-ui/**",
            "/swagger-resources/**",
            "/v2/**",
            "/health",
            "/health/liveness",
            "/status/health",
            "/loggers/**",
            "/");
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
            .exceptionHandling()
            .accessDeniedHandler((request, response, exc) -> response.sendError(HttpServletResponse.SC_FORBIDDEN))
            .authenticationEntryPoint((request, response, exc) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            .and()
            .authorizeRequests()
            .anyRequest()
            .authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(jwtAuthorityExtractor)
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
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

}
