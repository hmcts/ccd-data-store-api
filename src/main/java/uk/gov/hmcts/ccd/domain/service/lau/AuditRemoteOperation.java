package uk.gov.hmcts.ccd.domain.service.lau;

import uk.gov.hmcts.ccd.auditlog.AuditEntry;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

public interface AuditRemoteOperation {

    CompletableFuture<String> postCaseAction(AuditEntry entry, ZonedDateTime currentDateTime);

    CompletableFuture<String> postCaseSearch(AuditEntry entry, ZonedDateTime currentDateTime);
}
