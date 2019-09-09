package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.hmcts.reform.amlib.AccessManagementService;
import uk.gov.hmcts.reform.amlib.DefaultRoleSetupImportService;

import javax.sql.DataSource;

@Configuration
public class AccessManagementServiceConfiguration {

    private static final String DRIVER_CLASS = "org.postgresql.Driver";
    @Autowired
    ApplicationParams applicationParams;

    @Bean
    public AccessManagementService getAccessManagementService(@Qualifier("amDataSource") DataSource dataSource) {
        return new AccessManagementService(dataSource);
    }

    @Bean
    public DefaultRoleSetupImportService getDefaultRoleSetupImportService(@Qualifier("amDataSource") DataSource dataSource) {
        return new DefaultRoleSetupImportService(dataSource);
    }

    @Bean("amDataSource")
    public DataSource getAMDataSource() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName(DRIVER_CLASS);
        driverManagerDataSource.setUrl(applicationParams.getAmDBConnectionString());
        driverManagerDataSource.setUsername(applicationParams.getAmDBUserName());
        driverManagerDataSource.setPassword(applicationParams.getAmDBPassword());
        return driverManagerDataSource;
    }
}
