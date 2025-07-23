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

    public CaseDetails getCase(CaseDetails shellCase) {
        var uri = resolver.resolveUriOrThrow(shellCase.getReference());
        var response = api.getCases(uri, List.of(shellCase.getReference()));

        if (response.isEmpty()) {
            throw new CaseNotFoundException(String.valueOf(shellCase.getReference()));
        }

        var result = response.getFirst().getCaseDetails();
        // Decentralised services don't have our private ID and it isn't part of the decentralised contract.
        // We set it here for our internal use.
        result.setId(shellCase.getId());
        return result;
    }

    public CaseDetails createEvent(DecentralisedCaseEvent caseEvent) {
        var uri = resolver.resolveUriOrThrow(caseEvent.getCaseDetails());
        UUID idempotencyKey = idempotencyKeyHolder.getKey();

        if (idempotencyKey == null) {
            throw new IllegalStateException("No idempotency key set for the request context.");
        }

        return api.submitEvent(uri, idempotencyKey.toString(), caseEvent).getCaseDetails();
    }

    public List<AuditEvent> getCaseHistory(String caseReference) {
        var uri = resolver.resolveUriOrThrow(Long.valueOf(caseReference));
        return api.getCaseHistory(uri, caseReference)
            .stream()
            .map(DecentralisedAuditEvent::getEvent)
            .toList();
    }

    public AuditEvent getCaseHistoryEvent(String caseReference, Long eventId) {
        var uri = resolver.resolveUriOrThrow(Long.valueOf(caseReference));
        return api.getCaseHistoryEvent(uri, caseReference, eventId).getEvent();
    }

    public JsonNode updateSupplementaryData(String caseRef, SupplementaryDataUpdateRequest supplementaryData) {
        var uri = resolver.resolveUriOrThrow(Long.valueOf(caseRef));
        return api.updateSupplementaryData(uri, caseRef, supplementaryData).getSupplementaryData();
    }

}
