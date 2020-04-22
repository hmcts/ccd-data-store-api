package uk.gov.hmcts.ccd.auditlog;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class AuditEntry {

    private String dateTime;
    private int httpStatus;
    private String httpMethod;
    private String path;
    private String idamId;
    private String caseId;
    private String caseType;
    private String listOfCaseTypes;
    private String jurisdiction;
    private String eventSelected;
    private String invokingService;
    private String operationType;
    private String requestId;
    private String targetIdamId;
    private List<String> targetCaseRoles = new ArrayList<>();

}
