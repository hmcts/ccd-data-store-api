package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.amlib.AccessManagementService;

import javax.sql.DataSource;

@Configuration
public class AccessManagementServiceConfiguration {

    @Bean(name = "amDataSource")
    @ConfigurationProperties(prefix = "am.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
    @Bean
    public AccessManagementService getAccessManagementService(@Qualifier("amDataSource") DataSource dataSource) {
        return new AccessManagementService(dataSource);
    }
}
