package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLinksResource;
import uk.gov.hmcts.ccd.domain.service.casedeletion.CaseLinkService;
import uk.gov.hmcts.ccd.v2.V2;

@RestController
@RequestMapping(path = "/testing-support")
@ConditionalOnProperty(value = "testing.support.endpoint.enabled", havingValue = "false")
public class TestingSupportController {

    private CaseLinkService caseLinkService;

    @Autowired
    public TestingSupportController(CaseLinkService caseLinkService) {
        this.caseLinkService = caseLinkService;
    }

    @GetMapping(
        path = "/case-link/{caseReference}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        }
    )
    public ResponseEntity<CaseLinksResource> getCaseLink(@PathVariable("caseReference") String caseReference) {
        return ResponseEntity.ok(new CaseLinksResource(caseLinkService.findCaseLinks(caseReference)));
    }
}
