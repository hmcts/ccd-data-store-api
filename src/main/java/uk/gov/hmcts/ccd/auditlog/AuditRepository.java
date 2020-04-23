package uk.gov.hmcts.ccd.auditlog;

public interface AuditRepository {

    void save(AuditEntry auditEntry);
}
