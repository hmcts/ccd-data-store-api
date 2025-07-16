package uk.gov.hmcts.ccd.data.persistence;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients(basePackageClasses = {ServicePersistenceAPI.class})
@Configuration
class ServicePersistenceAPIConfiguration {
}
