package uk.gov.hmcts.ccd.domain.service.getevents;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;
import java.util.Optional;

public interface GetEventsOperation {
    List<AuditEvent> getEvents(CaseDetails caseDetails);

    List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference);

    List<AuditEvent> getEvents(String caseReference);

    Optional<AuditEvent> getEvent(CaseDetails caseDetails, String caseTypeId, Long eventId);
}
