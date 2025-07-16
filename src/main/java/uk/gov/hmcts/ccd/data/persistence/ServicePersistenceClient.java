package uk.gov.hmcts.ccd.data.persistence;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.ccd.domain.model.aggregated.DecentralisedCaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataUpdateRequest;

@RequiredArgsConstructor
@Service
public class ServicePersistenceClient {
    private final ServicePersistenceAPI api;

    public CaseDetails getCase(URI baseURI, String caseRef) {
        return api.getCase(baseURI, caseRef).getCaseDetails();
    }

    public CaseDetails createEvent(URI baseURI, DecentralisedCaseEvent caseEvent) {
        return api.createEvent(baseURI, caseEvent).getCaseDetails();
    }

    public List<AuditEvent> getCaseHistory(URI baseURI, String caseReference) {
        return api.getCaseHistory(baseURI, caseReference)
            .stream()
            .map(DecentralisedAuditEvent::getEvent)
            .toList();
    }

    public AuditEvent getCaseHistoryEvent(URI baseURI, String caseReference, Long eventId) {
        return api.getCaseHistoryEvent(baseURI, caseReference, eventId).getEvent();
    }

    public JsonNode updateSupplementaryData(URI baseURI, String caseRef, SupplementaryDataUpdateRequest supplementaryData) {
        return api.updateSupplementaryData(baseURI, caseRef, supplementaryData).getSupplementaryData();
    }

}
