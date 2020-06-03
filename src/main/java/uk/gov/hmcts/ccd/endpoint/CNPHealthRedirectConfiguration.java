package uk.gov.hmcts.ccd.endpoint;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * CNP Pipeline expects to see the Spring Boot Actuator Healthcheck
 * endpoint at /health, but (legacy) Zabbix expects it at /status/health
 * (cf https://tools.hmcts.net/confluence/display/RD/Exposing+Application+Status+to+Zabbix).
 *
 * <p>This redirect can be removed once Tactical env is decommissioned; we should configure the
 * endpoint at /health, and also remove /health and /status/** from
 *
 * <p>uk.gov.hmcts.ccd.definition.store.SecurityConfiguration
 *
 * <p>using Spring Actuators own management.security.enabled property
 */
@Configuration
public class CNPHealthRedirectConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/health").setViewName("forward:/status/health");
    }

}
