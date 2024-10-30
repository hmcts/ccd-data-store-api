package uk.gov.hmcts.ccd.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.ccd.customheaders.UserAuthHeadersInterceptorConfig;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@FeignClient(name = "pocApi", url = "${poc.apis.url}", configuration = UserAuthHeadersInterceptorConfig.class)
public interface PocApiClient {

    @GetMapping(value = "/ccd/cases/{case-ref}")
    CaseDetails getCase(@PathVariable("case-ref") String caseRef);

    @PostMapping(value = "/ccd/cases")
    CaseDetailsEntity createCase(@RequestBody CaseDetailsEntity caseDetails);
}
