package uk.gov.hmcts.ccd;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;

@Configuration
public class AccessManagementServiceConfiguration {

    @Bean
    public AccessManagementService getAccessManagementService() {
        return new AccessManagementService(getDriverManagerDataSource());
    }

    @Bean
    public DefaultRoleSetupImportService getDefaultRoleSetupImportService() {
        return new DefaultRoleSetupImportService(getDriverManagerDataSource());
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
