package uk.gov.hmcts.ccd.data.persistence;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@RequiredArgsConstructor
@Service
public class ServicePersistenceClient {
    private final ServicePersistenceAPI api;
    private final PersistenceStrategyResolver resolver;

    public CaseDetails getCase(String caseRef) {
        var uri = resolver.resolveUriOrThrow(caseRef);
        return api.getCase(uri, caseRef).getCaseDetails();
    }

    public CaseDetails createEvent(DecentralisedCaseEvent caseEvent) {
        var uri = resolver.resolveUriOrThrow(caseEvent.getCaseDetails());
        return api.createEvent(uri, caseEvent).getCaseDetails();
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
