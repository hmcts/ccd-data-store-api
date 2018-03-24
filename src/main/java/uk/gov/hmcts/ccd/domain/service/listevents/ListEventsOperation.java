package uk.gov.hmcts.ccd.domain.service.listevents;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import java.util.List;

public interface ListEventsOperation {
    List<AuditEvent> execute(CaseDetails caseDetails);
}
