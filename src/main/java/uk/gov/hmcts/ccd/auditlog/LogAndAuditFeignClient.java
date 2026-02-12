package uk.gov.hmcts.ccd.auditlog;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostResponse;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostResponse;
import uk.gov.hmcts.ccd.feign.FeignClientConfig;
import uk.gov.hmcts.ccd.domain.model.lau.CaseActionPostRequest;
import uk.gov.hmcts.ccd.domain.model.lau.CaseSearchPostRequest;

@FeignClient(name = "LogAndAuditFeignClient", url = "${lau.remote.case.audit.url}",
             configuration = FeignClientConfig.class)
public interface LogAndAuditFeignClient {

    @PostMapping("/audit/caseAction")
    ResponseEntity<CaseActionPostResponse> postCaseAction(
        @RequestBody CaseActionPostRequest caseActionPostRequest
    );

    @PostMapping("/audit/caseSearch")
    ResponseEntity<CaseSearchPostResponse> postCaseSearch(
        @RequestBody CaseSearchPostRequest caseSearchPostRequest
    );

}
