package uk.gov.hmcts.ccd.domain.service.lau;

import uk.gov.hmcts.ccd.auditlog.AuditEntry;

import java.time.ZonedDateTime;

public interface AuditRemoteOperation {

    void postCaseAction(AuditEntry entry, ZonedDateTime currentDateTime);

    void postCaseSearch(AuditEntry entry, ZonedDateTime currentDateTime);
}
