package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;

@Service
@Qualifier("default")
@RequiredArgsConstructor
public class DelegatingGetEventsOperation implements GetEventsOperation {
    private final PersistenceStrategyResolver resolver;
    private final DecentralisedGetEventsOperation decentralisedGetEventsOperation;
    private final LocalGetEventsOperation localGetEventsOperation;

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        if (resolver.isDecentralised(caseDetails)) {
            return decentralisedGetEventsOperation.getEvents(caseDetails);
        }
        return localGetEventsOperation.getEvents(caseDetails);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedGetEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference);
        }
        return localGetEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedGetEventsOperation.getEvents(caseReference);
        }
        return localGetEventsOperation.getEvents(caseReference);
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId) {
        if (resolver.isDecentralised(caseDetails)) {
            return decentralisedGetEventsOperation.getEvent(caseDetails, caseTypeId, eventId);
        }
        return localGetEventsOperation.getEvent(caseDetails, caseTypeId, eventId);
    }
}
