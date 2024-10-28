package uk.gov.hmcts.ccd.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@FeignClient(name = "pocApi", url = "${poc.apis.url}", configuration =
        FeignClientProperties.FeignClientConfiguration.class)
public interface PocApiClient {

    @GetMapping(value = "/ccd/cases/{case-ref}")
    CaseDetails getCase(@PathVariable("case-ref") String caseRef);
}
