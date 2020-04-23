package uk.gov.hmcts.ccd.auditlog;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class AuditLogFormatterTest {

    private AuditLogFormatter logFormatter = new AuditLogFormatter();

    @Test
    void shouldHaveCorrectLabels() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setDateTime("2020-12-05 10:30:45");
        auditEntry.setOperationType("VIEW_CASE");
        auditEntry.setCaseId("test_caseId");
        auditEntry.setIdamId("test_idamId");
        auditEntry.setInvokingService("test_invokingService");
        auditEntry.setHttpMethod("GET");
        auditEntry.setPath("test_path");
        auditEntry.setHttpStatus(200);
        auditEntry.setCaseType("test_caseType");
        auditEntry.setJurisdiction("test_jurisdiction");
        auditEntry.setEventSelected("test_eventSelected");
        auditEntry.setTargetIdamId("test_idamIdOfTarget");
        auditEntry.setTargetCaseRoles(Lists.newArrayList("test_targetCaseRoles"));
        auditEntry.setRequestId("test_X-Request-ID");

        String result = logFormatter.format(auditEntry);

        assertThat(result).containsOnlyOnce(AuditLogFormatter.TAG);

        assertThat(result).containsOnlyOnce("dateTime:2020-12-05 10:30:45");
        assertThat(result).containsOnlyOnce("operationType:VIEW_CASE");
        assertThat(result).containsOnlyOnce("caseId:test_caseId");
        assertThat(result).containsOnlyOnce("idamId:test_idamId");
        assertThat(result).containsOnlyOnce("invokingService:test_invokingService");
        assertThat(result).containsOnlyOnce("endpointCalled:GET test_path");
        assertThat(result).containsOnlyOnce("operationOutcome:200");
        assertThat(result).containsOnlyOnce("caseType:test_caseType");
        assertThat(result).containsOnlyOnce("jurisdiction:test_jurisdiction");
        assertThat(result).containsOnlyOnce("eventSelected:test_eventSelected");
        assertThat(result).containsOnlyOnce("idamIdOfTarget:test_idamIdOfTarget");
        assertThat(result).containsOnlyOnce("targetCaseRoles:test_targetCaseRoles");
        assertThat(result).containsOnlyOnce("X-Request-ID:test_X-Request-ID");
    }

    @Test
    void shouldNotLogPairIfEmpty() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setOperationType("VIEW_CASE");

        String result = logFormatter.format(auditEntry);

        assertThat(result).containsOnlyOnce("operationType:VIEW_CASE");
        assertThat(result).doesNotContainPattern("caseId:");
    }

    @Test
    void shouldHandleListWithComma() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setTargetCaseRoles(Lists.newArrayList("role1", "role2"));

        String result = logFormatter.format(auditEntry);

        assertThat(result).containsOnlyOnce("targetCaseRoles:role1,role2");
    }
}
