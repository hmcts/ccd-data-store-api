package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.persistence.ServicePersistenceClient;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Service
@RequiredArgsConstructor
public class DecentralisedGetEventsOperation implements AuditEventLoader {

    private final ServicePersistenceClient serviceClient;

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        return serviceClient.getCaseHistory(caseDetails.getReference());
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, Long eventId) {
        return Optional.of(serviceClient.getCaseHistoryEvent(caseDetails.getReference(), eventId));
    }
}
