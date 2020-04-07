package uk.gov.hmcts.ccd.auditlog;

public class LogMessage {

    private String dateTime;
    private int httpStatus;
    private String httpMethod;
    private String path;
    private String clientIp;
    private String javaMethod;
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

    public String getJavaMethod() {
        return javaMethod;
    }

    public void setJavaMethod(String javaMethod) {
        this.javaMethod = javaMethod;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "CLA-CCD " + dateTime +
            " { httpStatus=" + httpStatus +
            ", httpMethod='" + httpMethod + '\'' +
            ", path='" + path + '\'' +
            ", clientIp='" + clientIp + '\'' +
            ", javaMethod='" + javaMethod + '\'' +
            ", response='" + response + '\'' +
            '}';
    }

}
