package uk.gov.hmcts.ccd;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class AuditCaseRemoteConfiguration {

    @Value("${lau.remote.case.audit.enabled}")
    private boolean lauRemoteCaseAuditEnabled;

    @Value("${lau.remote.case.audit.url}")
    private String lauRemoteCaseAuditUrl;

    @Value("${lau.remote.case.audit.action.path}")
    private String lauRemoteCaseAuditActionPath;

    @Value("${lau.remote.case.audit.search.path}")
    private String lauRemoteCaseAuditSearchPath;

    public boolean isEnabled() {
        return this.lauRemoteCaseAuditEnabled;
    }

    public String getCaseActionAuditUrl() {
        return this.lauRemoteCaseAuditUrl + this.lauRemoteCaseAuditActionPath;
    }

    public String getCaseSearchAuditUrl() {
        return this.lauRemoteCaseAuditUrl + this.lauRemoteCaseAuditSearchPath;
    }

    @Bean(name = "httpClientAudit")
    public HttpClient httpClientAudit() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

}
