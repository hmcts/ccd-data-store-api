package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.ccd.auditlog.AuditService;
import uk.gov.hmcts.ccd.auditlog.AuditInterceptor;

@Configuration
public class AuditConfig implements WebMvcConfigurer {

    @Autowired
    private AuditService auditService;
    @Autowired
    private ApplicationParams applicationParams;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor());
    }

    @Bean
    public AuditInterceptor auditInterceptor() {
        return new AuditInterceptor(auditService, applicationParams);
    }

}
