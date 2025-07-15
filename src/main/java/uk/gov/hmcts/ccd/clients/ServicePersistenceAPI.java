package uk.gov.hmcts.ccd.clients;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.ccd.customheaders.ServicePersistenceAPIInterceptor;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.v2.external.dto.DecentralisedAuditEvent;

@FeignClient(name = "servicePersistenceAPI", configuration = ServicePersistenceAPIInterceptor.class)
public interface ServicePersistenceAPI {

    @GetMapping(value = "/ccd/cases/{case-ref}")
    CaseDetails getCase(URI baseURI, @PathVariable("case-ref") String caseRef);

    /**
     * Submits an event to create or update a case in CCD.
     *
     * <p><b>Idempotency</b></p>
     *
     * <p>Service endpoints are expected to be idempotent. The {@code Idempotency-Key} header is set
     * by a Feign client interceptor based on the event's token.
     *
     * <p><b>Server Behavior:</b></p>
     * <ul>
     *   <li><b>First Request:</b> When the server receives a request with a new idempotency key,
     *       it will process the event, create the case, and respond with an
     *       HTTP {@code 201 Created} status. The response body will contain the details of the
     *       newly created case.</li>
     *   <li><b>Subsequent Requests:</b> If the server receives a subsequent request with the
     *       <b>same idempotency key</b>, it will not re-process the event. Instead, it will
     *       retrieve the details of the previously created case and respond with an
     *       HTTP {@code 200 OK} status. The response body <b>>must be the same as that returned by the previously
     *       successful request.</b></li>
     * </ul>
     *
     */
    @PostMapping(value = "/ccd/cases", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CaseDetails createEvent(URI baseURI, @RequestBody DecentralisedCaseEvent caseEvent);

    @GetMapping(value = "/ccd/cases/{case-ref}/history")
    List<DecentralisedAuditEvent> getCaseHistory(URI baseURI, @PathVariable("case-ref") String caseReference);

    @GetMapping(value = "/ccd/cases/{case-ref}/history/{event-id}")
    DecentralisedAuditEvent getCaseHistoryEvent(URI baseURI,
                                   @PathVariable("case-ref") String caseReference,
                                   @PathVariable("event-id") Long eventId);

    @PostMapping(value = "/ccd/cases/{case-ref}/supplementary-data", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    JsonNode updateSupplementaryData(URI baseURI, @PathVariable("case-ref") String caseRef,
                                     SupplementaryDataUpdateRequest supplementaryData);

}
