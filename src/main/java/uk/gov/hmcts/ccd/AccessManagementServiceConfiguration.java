package uk.gov.hmcts.ccd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;

@Configuration
public class AccessManagementServiceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "am.datasource")
    public AccessManagementService getAccessManagementService() {
        DriverManagerDataSource driver = new DriverManagerDataSource();
        driver.setDriverClassName("org.postgresql.Driver");
        driver.setUrl("jdbc:postgresql://localhost:5500/am");
        driver.setUsername("amuser");
        driver.setPassword("ampass");

        return new AccessManagementService(driver);
    }
}
