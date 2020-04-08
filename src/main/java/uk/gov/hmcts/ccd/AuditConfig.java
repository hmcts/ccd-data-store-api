package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.auditlog.LogAuditInterceptor;

import java.time.Clock;

@Configuration
public class AuditConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private Clock clock;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logAuditInterceptor());
    }

    @Bean
    public LogAuditInterceptor logAuditInterceptor() {
        return new LogAuditInterceptor(auditService());
    }

    @Bean
    public AuditService auditService() {
        return new AuditService(clock);
    }
}
