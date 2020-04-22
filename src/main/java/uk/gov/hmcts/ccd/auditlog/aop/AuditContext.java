package uk.gov.hmcts.ccd.auditlog.aop;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;

import java.util.List;

@Builder(builderMethodName = "auditContextWith")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditContext {

    private String caseId;
    private String caseType;
    private String jurisdiction;
    private String eventName;
    private String targetIdamId;
    private List<String> targetCaseRoles;
    private AuditOperationType auditOperationType;

    private int httpStatus;
    private String httpMethod;
    private String requestPath;
    private String requestId;
}
