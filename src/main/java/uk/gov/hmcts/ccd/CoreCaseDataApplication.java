package uk.gov.hmcts.ccd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.DispatcherServlet;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.auditlog.LoggableDispatcherServlet;

import java.time.Clock;

@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
@EnableRetry
@ComponentScan({ "uk.gov.hmcts.ccd", "springfox" })
@EnableCaching
@EnableHypermediaSupport(type = { EnableHypermediaSupport.HypermediaType.HAL })
public class CoreCaseDataApplication {

    public static final String LOGGING_LEVEL_SPRINGFRAMEWORK = "logging.level.org.springframework.web";
    public static final String LOGGING_LEVEL_CCD = "logging.level.uk.gov.hmcts.ccd";

    protected CoreCaseDataApplication() {
    }

    public static void main(String[] args) {

        if (System.getProperty(LOGGING_LEVEL_CCD) != null) {
//            Configurator.setLevel(LOGGING_LEVEL_CCD, Level.valueOf(System.getProperty(LOGGING_LEVEL_CCD).toUpperCase()));
        }
        if (System.getProperty(LOGGING_LEVEL_SPRINGFRAMEWORK) != null) {
//            Configurator.setLevel(LOGGING_LEVEL_SPRINGFRAMEWORK, Level.valueOf(System.getProperty(LOGGING_LEVEL_SPRINGFRAMEWORK).toUpperCase()));
        }
        SpringApplication.run(CoreCaseDataApplication.class, args);
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ServletRegistrationBean dispatcherRegistration() {
        return new ServletRegistrationBean(dispatcherServlet());
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    public DispatcherServlet dispatcherServlet() {
        return new LoggableDispatcherServlet(auditService());
    }

    @Bean
    public AuditService auditService() {
        return new AuditService();
    }
}
