package uk.gov.hmcts.ccd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;

@Configuration
public class AccessManagementServiceConfiguration {

    @Bean
    public AccessManagementService getAccessManagementService() {
        DriverManagerDataSource driver = getDriverManagerDataSource();
        return new AccessManagementService(driver);
    }

    @Bean
    public DefaultRoleSetupImportService getDefaultRoleSetupImportService() {
        DriverManagerDataSource driver = getDriverManagerDataSource();
        return new DefaultRoleSetupImportService(driver);
    }

    private DriverManagerDataSource getDriverManagerDataSource() {
        DriverManagerDataSource driver = new DriverManagerDataSource();
        driver.setDriverClassName("org.postgresql.Driver");
        driver.setUrl("jdbc:postgresql://localhost:5500/am");
        driver.setUsername("amuser");
        driver.setPassword("ampass");
        return driver;
    }
}
