package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
@Qualifier("decentralised")
@RequiredArgsConstructor
public class DecentralisedGetEventsOperation implements GetEventsOperation {

    private final ServicePersistenceClient serviceClient;

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        return serviceClient.getCaseHistory(caseDetails.getReferenceAsString());
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        return serviceClient.getCaseHistory(caseReference);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        return serviceClient.getCaseHistory(caseReference);
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId) {
        return Optional.of(serviceClient.getCaseHistoryEvent(caseDetails.getReferenceAsString(),
            eventId));
    }
}
