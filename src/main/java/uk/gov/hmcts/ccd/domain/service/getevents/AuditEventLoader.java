package uk.gov.hmcts.ccd.domain.service.getevents;

import java.util.List;
import java.util.Optional;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

interface AuditEventLoader {
    List<AuditEvent> getEvents(CaseDetails caseDetails);
    Optional<AuditEvent> getEvent(CaseDetails caseDetails, Long eventId);
}
