package uk.gov.hmcts.ccd;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class AliasWebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        /**
         * This alias is to support Tactical health checks. Once support of Tactical is dropped,
         * this alias can be removed.
         */
        registry.addViewController("/status/health").setViewName("forward:/health");
        registry.addViewController("/").setViewName("forward:/health");
    }
}
