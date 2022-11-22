package uk.gov.hmcts.ccd;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.security.DataStoreHttpStatusRequestRejectedHandler;

@Configuration
public class FirewallConfiguration {

    @Bean
    public RequestRejectedHandler requestRejectedHandler(AppInsights appInsights) {
        return new DataStoreHttpStatusRequestRejectedHandler(appInsights);
    }
}
