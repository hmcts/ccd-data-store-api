package uk.gov.hmcts.ccd.customheaders;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.ccd.ApplicationParams;

@Configuration
public class CustomHeadersFilterConfig {

    private final ApplicationParams applicationParams;

    public CustomHeadersFilterConfig(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public FilterRegistrationBean<CustomHeadersFilter> customHeadersFilter() {
        FilterRegistrationBean<CustomHeadersFilter> customHeadersFilter = new FilterRegistrationBean<>();
        customHeadersFilter.setFilter(new CustomHeadersFilter(applicationParams));
        customHeadersFilter.addUrlPatterns("/**");
        customHeadersFilter.setOrder(0);
        return customHeadersFilter;
    }

    @Bean
    public CustomHeadersFilter customHeadersFilterBean() {
        return new CustomHeadersFilter(applicationParams);
    }
}
