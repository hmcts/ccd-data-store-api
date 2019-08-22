package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;

@Configuration
public class AccessManagementServiceConfiguration {
    private ApplicationParams applicationParams;
    private final String driverClassName = "org.postgresql.Driver";

    @Autowired
    AccessManagementServiceConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public AccessManagementService getAccessManagementService() {
        return new AccessManagementService(getAccessManagementDataSource());
    }

    @Bean
    public DefaultRoleSetupImportService getDefaultRoleSetupImportService() {
        return new DefaultRoleSetupImportService(getAccessManagementDataSource());
    }

    private DriverManagerDataSource getAccessManagementDataSource() {
        DriverManagerDataSource driver = new DriverManagerDataSource();

        driver.setDriverClassName(driverClassName);
        driver.setUrl(applicationParams.getAmDBConnectionString());
        driver.setUsername(applicationParams.getAmDBUserName());
        driver.setPassword(applicationParams.getAmDBPassword());
        return driver;
    }
}
