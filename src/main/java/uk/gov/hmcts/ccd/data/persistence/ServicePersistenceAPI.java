package uk.gov.hmcts.ccd.data.persistence;

import java.net.URI;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedAuditEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedSubmitEventResponse;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedUpdateSupplementaryDataResponse;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

@FeignClient(name = "servicePersistenceAPI", configuration = ServicePersistenceAPIInterceptor.class)
interface ServicePersistenceAPI {


    /**
     * Submits an event to create or update a case.
     *
     * <p><b>Idempotency</b></p>
     *
     * <p>Services are expected to ensure idempotency based on the associated idempotency key.
     *
     * <p><b>Service Behavior:</b></p>
     * <ul>
     *   <li><b>First Request:</b> When the service receives a request with a new idempotency key,
     *       it will process the event, create the case, and respond with an
     *       HTTP {@code 201 Created} status. The response body will contain the details of the
     *       newly created case.</li>
     *   <li><b>Subsequent Requests:</b> If the service receives a subsequent request with the
     *       <b>same idempotency key</b>, it will not re-process the event. Instead, it will
     *       retrieve the details of the previously created case and respond with an
     *       HTTP {@code 200 OK} status. The response body <b>>must be the same as that returned by
     *       the previously successful request.</b></li>
     * </ul>
     *
     */
    @PostMapping(value = "/ccd-persistence/cases", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    DecentralisedSubmitEventResponse submitEvent(URI baseURI,
                                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                 @RequestBody DecentralisedCaseEvent caseEvent);

    @PostMapping(value = "/ccd-persistence/cases/{case-ref}/supplementary-data",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    DecentralisedUpdateSupplementaryDataResponse updateSupplementaryData(
        URI baseURI,
        @PathVariable("case-ref") Long caseRef,
        SupplementaryDataUpdateRequest supplementaryData);

    @GetMapping(value = "/ccd-persistence/cases")
    List<DecentralisedCaseDetails> getCases(URI baseURI, @RequestParam("case-refs") List<Long> caseRefs);

    @GetMapping(value = "/ccd-persistence/cases/{case-ref}/history")
    List<DecentralisedAuditEvent> getCaseHistory(URI baseURI, @PathVariable("case-ref") Long caseReference);

    @GetMapping(value = "/ccd-persistence/cases/{case-ref}/history/{event-id}")
    DecentralisedAuditEvent getCaseHistoryEvent(URI baseURI,
                                   @PathVariable("case-ref") Long caseReference,
                                   @PathVariable("event-id") Long eventId);
}
