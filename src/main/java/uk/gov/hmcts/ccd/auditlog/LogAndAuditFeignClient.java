package uk.gov.hmcts.ccd.auditlog;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import uk.gov.hmcts.ccd.config.LogAndAuditFeignHttpConfig;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;

@FeignClient(name = "LogAndAuditFeignClient", configuration = LogAndAuditFeignHttpConfig.class)
public interface LogAndAuditFeignClient {

    @RequestLine("POST /audit/caseAction")
    @Headers({
        "Content-Type: application/json",
        "Accept: application/json",
        "ServiceAuthorization: {ServiceAuthorization}"
    })
    Response postCaseAction(@Param("ServiceAuthorization") String serviceAuthorization,
                            CaseActionPostRequest caseActionPostRequest);

    @RequestLine("POST /audit/caseSearch")
    @Headers({
        "Content-Type: application/json",
        "Accept: application/json",
        "ServiceAuthorization: {ServiceAuthorization}"
    })
    Response postCaseSearch(@Param("ServiceAuthorization") String serviceAuthorization,
                            CaseSearchPostRequest caseSearchPostRequest);

}
