package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;

@Configuration
public class AccessManagementServiceConfiguration {

    @Autowired
    private ApplicationParams applicationParams;

    private static final String DRIVER_CLASS = "org.postgresql.Driver";

    private DriverManagerDataSource getDriverManagerDataSource() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();

        driverManagerDataSource.setDriverClassName(DRIVER_CLASS);
        driverManagerDataSource.setUrl(applicationParams.getAmDBConnectionString());
        driverManagerDataSource.setUsername(applicationParams.getAmDBUserName());
        driverManagerDataSource.setPassword(applicationParams.getAmDBPassword());
        return driverManagerDataSource;
    }

    @Autowired
    AccessManagementServiceConfiguration(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Bean
    public AccessManagementService getAccessManagementService() {
        return new AccessManagementService(getDriverManagerDataSource());
    }

    @Bean
    public DefaultRoleSetupImportService getDefaultRoleSetupImportService() {
        return new DefaultRoleSetupImportService(getDriverManagerDataSource());
    }
}
