package uk.gov.hmcts.ccd.clients;

import java.net.URI;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.ccd.customheaders.UserAuthHeadersInterceptorConfig;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.v2.external.resource.SupplementaryDataResource;

@FeignClient(name = "servicePersistenceAPI", configuration = UserAuthHeadersInterceptorConfig.class)
public interface ServicePersistenceAPI {

    @GetMapping(value = "/ccd/cases/{case-ref}")
    CaseDetails getCase(URI baseURI, @PathVariable("case-ref") String caseRef);

    @PostMapping(value = "/ccd/cases", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CaseDetails createEvent(URI baseURI, @RequestBody DecentralisedCaseEvent caseEvent);

    @GetMapping(value = "/ccd/cases/{case-ref}/history")
    List<AuditEvent> getCaseHistory(URI baseURI, @PathVariable("case-ref") String caseReference);

    @PostMapping(value = "/ccd/cases/{case-ref}/supplementary-data", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    SupplementaryData updateSupplementaryData(URI baseURI, @PathVariable("case-ref") String caseRef,
                                                      SupplementaryDataUpdateRequest supplementaryData);

    }
