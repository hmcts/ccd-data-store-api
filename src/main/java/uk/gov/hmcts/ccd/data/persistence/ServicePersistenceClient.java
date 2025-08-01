package uk.gov.hmcts.ccd.data.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@RequiredArgsConstructor
@Service
@Slf4j
public class ServicePersistenceClient {
    private final ServicePersistenceAPI api;
    private final PersistenceStrategyResolver resolver;
    private final IdempotencyKeyHolder idempotencyKeyHolder;

    /**
     * Retrieves case details from a decentralised service.
     */
    public CaseDetails getCase(CaseDetails casePointer) {
        var uri = resolver.resolveUriOrThrow(casePointer.getReference());
        var response = api.getCases(uri, List.of(casePointer.getReference()));

        if (response.isEmpty()) {
            throw new CaseNotFoundException(String.valueOf(casePointer.getReference()));
        }

        var first = response.getFirst();

        validateCaseDetails(casePointer, first);
        var returnedCaseDetails = first.getCaseDetails();

        // The decentralised service doesn't know about our internal ID. We enrich the object here for internal use.
        returnedCaseDetails.setId(casePointer.getId());
        return returnedCaseDetails;
    }

    /**
     * Submits a create or update event to a decentralised service.
     *
     * <p>This method handles the full interaction, including idempotency, error/warning processing, and a critical
     * security validation to ensure the response from the service corresponds to the case the event was for.
     *
     * @param caseEvent The event payload containing the case details to be submitted.
     * @return The final state of the {@link CaseDetails} after the event is processed.
     * @throws IllegalStateException if no idempotency key is available in the request context.
     * @throws ApiException if the remote service returns validation errors or warnings.
     * @throws ServiceException if the remote service returns mismatched case identity information.
     */
    public DecentralisedCaseDetails createEvent(DecentralisedCaseEvent caseEvent) {
        var casePointer = caseEvent.getCaseDetails();
        var uri = resolver.resolveUriOrThrow(casePointer);
        UUID idempotencyKey = idempotencyKeyHolder.getKey();

        if (idempotencyKey == null) {
            throw new IllegalStateException("No idempotency key set for the request context.");
        }

        DecentralisedSubmitEventResponse response = api.submitEvent(uri, idempotencyKey.toString(), caseEvent);

        // Handle functional errors and warnings returned by the service
        if (!isEmpty(response.getErrors())
            || (!isEmpty(response.getWarnings())
            && (response.getIgnoreWarning() == null || !response.getIgnoreWarning()))) {
            throw new ApiException("Unable to proceed because there are one or more callback Errors or Warnings")
                .withErrors(response.getErrors())
                .withWarnings(response.getWarnings());
        }

        var details = response.getCaseDetails();

        validateCaseDetails(casePointer, details);

        // The decentralised service doesn't know about our internal ID. We enrich the object here for internal use.
        details.getCaseDetails().setId(casePointer.getId());
        return details;
    }

    public List<AuditEvent> getCaseHistory(CaseDetails caseDetails) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        return api.getCaseHistory(uri, caseDetails.getReference())
            .stream()
            .map(x -> x.getEvent(caseDetails.getId()))
            .toList();
    }

    public AuditEvent getCaseHistoryEvent(CaseDetails caseDetails, Long eventId) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        return api.getCaseHistoryEvent(uri, caseDetails.getReference(), eventId).getEvent(caseDetails.getId());
    }

    public JsonNode updateSupplementaryData(Long caseRef, SupplementaryDataUpdateRequest supplementaryData) {
        var uri = resolver.resolveUriOrThrow(caseRef);
        return api.updateSupplementaryData(uri, caseRef, supplementaryData).getSupplementaryData();
    }

    private void validateCaseDetails(CaseDetails casePointer, DecentralisedCaseDetails details) {
        if (details.getVersion() == null) {
            throw new ServiceException("Downstream service failed to return a version for case reference "
                + casePointer.getReference());
        }
        var returnedCaseDetails = details.getCaseDetails();
        if (!casePointer.getReference().equals(returnedCaseDetails.getReference())
            || !casePointer.getCaseTypeId().equals(returnedCaseDetails.getCaseTypeId())) {

            log.error("""
                 Downstream service returned mismatched case details Expected ref={} type={}, but got ref={} type={}""",
                casePointer.getReference(),
                casePointer.getCaseTypeId(),
                returnedCaseDetails.getReference(),
                returnedCaseDetails.getCaseTypeId());

            throw new ServiceException("Downstream service returned mismatched case details for case reference "
                + casePointer.getReference());
        }
    }
}
