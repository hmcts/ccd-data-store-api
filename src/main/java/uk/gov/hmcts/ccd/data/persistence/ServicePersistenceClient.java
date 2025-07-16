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
import uk.gov.hmcts.ccd.infrastructure.IdempotencyKeyHolder;

@RequiredArgsConstructor
@Service
public class ServicePersistenceClient {
    private final ServicePersistenceAPI api;
    private final PersistenceStrategyResolver resolver;
    private final IdempotencyKeyHolder idempotencyKeyHolder;

    public CaseDetails getCase(String caseRef) {
        var uri = resolver.resolveUriOrThrow(caseRef);
        return api.getCase(uri, caseRef).getCaseDetails();
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
        var uri = resolver.resolveUriOrThrow(caseReference);
        return api.getCaseHistory(uri, caseReference)
            .stream()
            .map(DecentralisedAuditEvent::getEvent)
            .toList();
    }

    public AuditEvent getCaseHistoryEvent(String caseReference, Long eventId) {
        var uri = resolver.resolveUriOrThrow(caseReference);
        return api.getCaseHistoryEvent(uri, caseReference, eventId).getEvent();
    }

    public JsonNode updateSupplementaryData(String caseRef, SupplementaryDataUpdateRequest supplementaryData) {
        var uri = resolver.resolveUriOrThrow(caseRef);
        return api.updateSupplementaryData(uri, caseRef, supplementaryData).getSupplementaryData();
    }

}
