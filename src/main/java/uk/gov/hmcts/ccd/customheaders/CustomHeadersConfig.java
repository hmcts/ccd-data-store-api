package uk.gov.hmcts.ccd.customheaders;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.ccd.ApplicationParams;

@Configuration
public class CustomHeadersConfig implements WebMvcConfigurer {

    private ApplicationParams applicationParams;

    public CustomHeadersConfig(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customHeadersInterceptor());
    }

    @Bean
    public CustomHeadersInterceptor customHeadersInterceptor() {
        return new CustomHeadersInterceptor(applicationParams);
    }

}
