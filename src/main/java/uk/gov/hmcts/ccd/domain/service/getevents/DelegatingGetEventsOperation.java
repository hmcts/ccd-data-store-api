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
        return getOperation(caseDetails.getReferenceAsString())
            .getEvents(caseDetails);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        return getOperation(caseReference)
            .getEvents(jurisdiction, caseTypeId, caseReference);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        return getOperation(caseReference)
            .getEvents(caseReference);
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId) {
        return getOperation(caseDetails.getReferenceAsString())
            .getEvent(caseDetails, caseTypeId, eventId);
    }

    private GetEventsOperation getOperation(String caseReference) {
        if (resolver.isDecentralised(Long.valueOf(caseReference))) {
            return decentralisedGetEventsOperation;
        }
        return localGetEventsOperation;
    }
}
