package uk.gov.hmcts.ccd.feign;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FeignHttpMessageConvertersConfiguration {

    /**
     * Eagerly initializes the converters before publishing the bean to concurrent Feign clients.
     *
     * <p>Remove this workaround after upgrading to Spring Cloud OpenFeign 5.0.3 or later, which
     * includes the upstream concurrency fix from spring-cloud-openfeign#1371.
     */
    @Bean
    public FeignHttpMessageConverters feignHttpMessageConverters(
        ObjectProvider<ClientHttpMessageConvertersCustomizer> customizers,
        ObjectProvider<HttpMessageConverterCustomizer> cloudCustomizers
    ) {
        FeignHttpMessageConverters converters = new FeignHttpMessageConverters(customizers, cloudCustomizers);
        converters.getConverters();
        return converters;
    }
}
