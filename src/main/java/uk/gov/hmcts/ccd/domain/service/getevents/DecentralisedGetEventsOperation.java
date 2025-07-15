package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.clients.ServicePersistenceAPI;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.v2.external.dto.DecentralisedAuditEvent;

@Service
@Qualifier("decentralised")
@RequiredArgsConstructor
public class DecentralisedGetEventsOperation implements GetEventsOperation {

    private final PersistenceStrategyResolver resolver;
    private final ServicePersistenceAPI servicePersistenceAPI;

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        return servicePersistenceAPI.getCaseHistory(uri, caseDetails.getReferenceAsString())
            .stream().map(DecentralisedAuditEvent::getEvent).toList();
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        var uri = resolver.resolveUriOrThrow(caseReference);
        return servicePersistenceAPI.getCaseHistory(uri, caseReference)
            .stream().map(DecentralisedAuditEvent::getEvent).toList();
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        var uri = resolver.resolveUriOrThrow(caseReference);
        return servicePersistenceAPI.getCaseHistory(uri, caseReference)
            .stream().map(DecentralisedAuditEvent::getEvent).toList();
    }

    @Override
    public Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId) {
        var uri = resolver.resolveUriOrThrow(caseDetails);
        return Optional.of(servicePersistenceAPI.getCaseHistoryEvent(uri, caseDetails.getReferenceAsString(),
            eventId).getEvent());
    }
}
