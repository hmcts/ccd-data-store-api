package uk.gov.hmcts.ccd.data.persistence;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@RequiredArgsConstructor
@Service
public class ServicePersistenceClient {
    private final ServicePersistenceAPI api;
    private final PersistenceStrategyResolver resolver;
    private final IdempotencyKeyHolder idempotencyKeyHolder;

    public CaseDetails getCase(CaseDetails casePointer) {
        var uri = resolver.resolveUriOrThrow(casePointer.getReference());
        var response = api.getCases(uri, List.of(casePointer.getReference()));

        if (response.isEmpty()) {
            throw new CaseNotFoundException(String.valueOf(casePointer.getReference()));
        }

        var result = response.getFirst().getCaseDetails();
        // Decentralised services don't have our private ID and it isn't part of the decentralised contract.
        // We set it here for our internal use.
        result.setId(casePointer.getId());
        return result;
    }

    public CaseDetails createEvent(DecentralisedCaseEvent caseEvent) {
        var uri = resolver.resolveUriOrThrow(caseEvent.getCaseDetails());
        UUID idempotencyKey = idempotencyKeyHolder.getKey();

        if (idempotencyKey == null) {
            throw new IllegalStateException("No idempotency key set for the request context.");
        }

        var result = api.submitEvent(uri, idempotencyKey.toString(), caseEvent).getCaseDetails();
        result.setId(caseEvent.getCaseDetails().getId());
        return result;
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

}
