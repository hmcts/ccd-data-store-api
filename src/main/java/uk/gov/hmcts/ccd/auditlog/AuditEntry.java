package uk.gov.hmcts.ccd.auditlog;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.builder.ToStringStyle.JSON_STYLE;

public class AuditEntry {

    private static final String TAG = "CLA-CCD";

    private String dateTime;
    private int httpStatus;
    private String httpMethod;
    private String path;
    private String idamId;
    private String caseId;
    private String caseType;
    private String jurisdiction;
    private String eventSelected;
    private String invokingService;
    private String operationType;
    private String requestId;
    private String targetIdamId;
    private List<String> targetCaseRoles;

    public void setDateTime(String time) {
        this.dateTime = time;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getIdamId() {
        return idamId;
    }

    public void setIdamId(String idamId) {
        this.idamId = idamId;
    }

    public String getInvokingService() {
        return invokingService;
    }

    public void setInvokingService(String invokingService) {
        this.invokingService = invokingService;
    }

    public static String getTAG() {
        return TAG;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getEventSelected() {
        return eventSelected;
    }

    public void setEventSelected(String eventSelected) {
        this.eventSelected = eventSelected;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTargetIdamId() {
        return targetIdamId;
    }

    public void setTargetIdamId(String targetIdamId) {
        this.targetIdamId = targetIdamId;
    }

    public List<String> getTargetCaseRoles() {
        return targetCaseRoles;
    }

    public void setTargetCaseRoles(List<String> targetCaseRoles) {
        this.targetCaseRoles = targetCaseRoles;
    }

    @Override
    public String toString() {
        return TAG + " " + dateTime + " operationType:" + operationType +
            (isNotBlank(caseId) ? ", caseId:" + caseId : "") +
            (isNotBlank(idamId) ? ", idamId:" + idamId : "") +
            (isNotBlank(invokingService) ? ", invokingService:" + invokingService : "") +
            (isNotBlank(path) ? ", endpointCalled:" + httpMethod + " " + path : "") +
            ", operationOutcome:" + httpStatus +
            (isNotBlank(caseType) ? ", caseType:" + caseType : "") +
            (isNotBlank(jurisdiction) ? ", jurisdiction:" + jurisdiction : "") +
            (isNotBlank(eventSelected) ? ", eventSelected:" + eventSelected : "") +
           (isNotBlank(targetIdamId) ? ", idamIdOfTarget:" + targetIdamId : "") +
//            (isNotBlank(listOfCaseTypes) ? ", listOfCaseTypes:" + listOfCaseTypes : "") +
            (!targetCaseRoles.isEmpty() ? ", targetCaseRoles:" +
                targetCaseRoles.stream().map(String::toString).collect(Collectors.joining(",")) : "") +
            (isNotBlank(requestId) ? ", X-Request-ID:" + requestId : "") +
            '}';
    }

    public String formattedAuditData() {
        return  ReflectionToStringBuilder.toString(this, JSON_STYLE, false, false,
            true, null).toString();
    }
}
