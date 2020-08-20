package uk.gov.hmcts.ccd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseRoleRepository;
import uk.gov.hmcts.ccd.logging.CorrelationIDExtractor;
import uk.gov.hmcts.ccd.logging.CorrelationIDHttpExtractor;
import uk.gov.hmcts.ccd.logging.MdcInterceptor;

@Configuration
public class CorrelationIDConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier("CorrelationIDHttpExtractor")
    private CorrelationIDExtractor correlationIDExtractor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MdcInterceptor(correlationIDExtractor));
    }
}
