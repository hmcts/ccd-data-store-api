package uk.gov.hmcts.ccd.auditlog;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


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

        assertEquals("CLA-CCD dateTime:2020-12-05 10:30:45,"
            + "operationType:VIEW_CASE,"
            + "caseId:test_caseId,"
            + "idamId:test_idamId,"
            + "invokingService:test_invokingService,"
            + "endpointCalled:GET test_path,"
            + "operationalOutcome:200,"
            + "caseType:test_caseType,"
            + "jurisdiction:test_jurisdiction,"
            + "eventSelected:test_eventSelected,"
            + "idamIdOfTarget:test_idamIdOfTarget,"
            + "targetCaseRoles:test_targetCaseRoles,"
            + "X-Request-ID:test_X-Request-ID", result);
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

    @Test
    void shouldHandleNullTargetCaseRoles() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setTargetCaseRoles(null);

        String result = logFormatter.format(auditEntry);

        assertThat(result).doesNotContain("targetCaseRoles:role1,role2");
    }
}
