package uk.gov.hmcts.ccd.customheaders;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.ApplicationParams;

@Configuration
public class CustomHeadersConfig {

    private ApplicationParams applicationParams;

    public CustomHeadersConfig(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean(name = "customHeadersFilterBean")
    public FilterRegistrationBean<CustomHeadersFilter> customHeadersFilterBean() {
        FilterRegistrationBean<CustomHeadersFilter> customHeadersFilterBean = new FilterRegistrationBean<>();
        customHeadersFilterBean.setFilter(new CustomHeadersFilter(applicationParams));
        customHeadersFilterBean.addUrlPatterns("/*");
        return customHeadersFilterBean;
    }
}
