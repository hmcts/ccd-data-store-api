package uk.gov.hmcts.ccd.data.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedAuditEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseDetails;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.data.persistence.dto.DecentralisedSubmitEventResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

import java.util.List;
import java.util.Objects;
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
        var uri = resolver.resolveUriOrThrow(casePointer);
        var response = api.getCases(uri, List.of(casePointer.getReference()));

        if (response.isEmpty()) {
            throw new CaseNotFoundException(String.valueOf(casePointer.getReference()));
        }

        var first = response.getFirst();

        validateCaseDetails(casePointer, first);
        var returnedCaseDetails = first.getCaseDetails();
        returnedCaseDetails.setRevision(first.getRevision());

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
            var errors = response.getErrors();
            var warnings = response.getWarnings();
            int errorCount = errors == null ? 0 : errors.size();
            int warningCount = warnings == null ? 0 : warnings.size();
            log.warn("Decentralised submit rejected for case {} event {} with {} error(s) and {} warning(s)",
                casePointer.getReference(),
                caseEvent.getEventDetails().getEventId(),
                errorCount,
                warningCount);
            throw new ApiException("Unable to proceed because there are one or more callback Errors or Warnings")
                .withErrors(response.getErrors())
                .withWarnings(response.getWarnings());
        }

        var details = response.getCaseDetails();

        validateCaseDetails(casePointer, details);

        details.getCaseDetails().setRevision(details.getRevision());

        // The decentralised service doesn't know about our internal ID. We enrich the object here for internal use.
        details.getCaseDetails().setId(casePointer.getId());
        return details;
    }

    public List<AuditEvent> getCaseHistory(CaseDetails caseDetails) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        return api.getCaseHistory(uri, caseDetails.getReference())
            .stream()
            .map(auditEvent -> extractValidatedAuditEvent(caseDetails, auditEvent))
            .toList();
    }

    public AuditEvent getCaseHistoryEvent(CaseDetails caseDetails, Long eventId) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        var auditEvent = api.getCaseHistoryEvent(uri, caseDetails.getReference(), eventId);
        return extractValidatedAuditEvent(caseDetails, auditEvent);
    }

    public JsonNode updateSupplementaryData(Long caseRef, SupplementaryDataUpdateRequest supplementaryData) {
        var uri = resolver.resolveUriOrThrow(caseRef);
        return api.updateSupplementaryData(uri, caseRef, supplementaryData).getSupplementaryData();
    }

    private void validateCaseDetails(CaseDetails casePointer, DecentralisedCaseDetails details) {
        if (details.getRevision() == null) {
            throw new ServiceException("Downstream service failed to return a revision for case reference "
                + casePointer.getReference());
        }
        var returnedCaseDetails = details.getCaseDetails();
        if (!Objects.equals(casePointer.getReference(), returnedCaseDetails.getReference())
            || !Objects.equals(casePointer.getCaseTypeId(), returnedCaseDetails.getCaseTypeId())
            || !Objects.equals(casePointer.getJurisdiction(), returnedCaseDetails.getJurisdiction())) {

            log.error("""
                 Downstream service returned mismatched case details.
                 Expected ref={} type={} jurisdiction={},
                 but got ref={} type={} jurisdiction={}""",
                casePointer.getReference(),
                casePointer.getCaseTypeId(),
                casePointer.getJurisdiction(),
                returnedCaseDetails.getReference(),
                returnedCaseDetails.getCaseTypeId(),
                returnedCaseDetails.getJurisdiction());

            throw new ServiceException("Downstream service returned mismatched case details for case reference "
                + casePointer.getReference());
        }
    }

    private AuditEvent extractValidatedAuditEvent(CaseDetails casePointer, DecentralisedAuditEvent auditEvent) {
        if (!Objects.equals(casePointer.getReference(), auditEvent.getCaseReference())) {
            log.error("Downstream service returned audit event for case reference {} but CCD expected {}",
                auditEvent.getCaseReference(), casePointer.getReference());
            throw new ServiceException("Downstream service returned audit event for unexpected case reference "
                + casePointer.getReference());
        }

        AuditEvent event = auditEvent.getEvent(casePointer.getId());

        if (!Objects.equals(casePointer.getCaseTypeId(), event.getCaseTypeId())) {
            log.error("Downstream service returned audit event for case type {} but CCD expected {}",
                event.getCaseTypeId(), casePointer.getCaseTypeId());
            throw new ServiceException("Downstream service returned audit event for unexpected case type "
                + casePointer.getCaseTypeId());
        }

        return event;
    }
}
