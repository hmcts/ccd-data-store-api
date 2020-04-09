package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.auditlog.LogAuditInterceptor;

@Configuration
public class AuditConfig implements WebMvcConfigurer {

    @Autowired
    private AuditService auditService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logAuditInterceptor());
    }

    @Bean
    public LogAuditInterceptor logAuditInterceptor() {
        return new LogAuditInterceptor(auditService);
    }

}
