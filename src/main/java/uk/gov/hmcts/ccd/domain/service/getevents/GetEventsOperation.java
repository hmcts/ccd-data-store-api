package uk.gov.hmcts.ccd.domain.service.getevents;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;
import java.util.Optional;

public interface GetEventsOperation {
    List<AuditEvent> execute(CaseDetails caseDetails);

    List<AuditEvent> execute(String jurisdiction, String caseTypeId, String caseReference);

    Optional<AuditEvent> execute(String jurisdiction, String caseTypeId, Long eventId);
}
