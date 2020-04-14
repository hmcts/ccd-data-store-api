package uk.gov.hmcts.ccd.auditlog;

public class AuditEntry {

    private static final String TAG = "CLA-CCD";

    private String dateTime;
    private int httpStatus;
    private String httpMethod;
    private String path;
    private String clientIp;
    private String idamId;
    private String caseId;
    private String caseType;;
    private String jurisdiction;
    private String invokingService;
    private String operationType;
    private String response;

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

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
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

    @Override
    public String toString() {
        return TAG + " " + dateTime + " Operation:" + operationType +
            " { httpStatus=" + httpStatus +
            ", httpMethod='" + httpMethod + '\'' +
            ", path='" + path + '\'' +
            ", clientIp='" + clientIp + '\'' +
            ", idamId='" + idamId + '\'' +
            ", invokingService='" + invokingService + '\'' +
            ", caseId='" + caseId + '\'' +
            ", caseType='" + caseType + '\'' +
            ", jurisdiction='" + jurisdiction + '\'' +
            ", response='" + response + '\'' +
            '}';
    }

}
