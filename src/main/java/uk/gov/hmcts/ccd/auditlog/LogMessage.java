package uk.gov.hmcts.ccd.auditlog;

public class LogMessage {
    private static final String TAG = "CLA-CCD";
    private String dateTime;

    private int httpStatus;
    private String httpMethod;
    private String path;
    private String clientIp;
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

    @Override
    public String toString() {
        return TAG + " " + dateTime + " Operation:" + operationType +
            " { httpStatus=" + httpStatus +
            ", httpMethod='" + httpMethod + '\'' +
            ", path='" + path + '\'' +
            ", clientIp='" + clientIp + '\'' +
            ", response='" + response + '\'' +
            '}';
    }

}
