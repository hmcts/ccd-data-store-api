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

    public static final int MAX_CASE_IDS_LIST = 10;
    public static final String CASE_ID_SEPARATOR = ",";

    private String caseId;
    private String caseType;
    private String caseTypeIds;
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
