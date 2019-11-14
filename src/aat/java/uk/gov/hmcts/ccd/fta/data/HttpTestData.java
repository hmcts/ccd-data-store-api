package uk.gov.hmcts.ccd.fta.data;

import java.util.List;

public class HttpTestData {

    private String _guid_;

    private String _extends_;

    private String title;

    private List<String> specs;

    private String productName;

    private String operationName;

    private UserData user;

    private String method;

    private String uri;

    private RequestData request;

    private ResponseData expectedResponse;

    public String get_guid_() {
        return _guid_;
    }

    public void set_guid_(String _guid_) {
        this._guid_ = _guid_;
    }

    public String get_extends_() {
        return _extends_;
    }

    public void set_extends_(String _extends_) {
        this._extends_ = _extends_;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getSpecs() {
        return specs;
    }

    public void setSpecs(List<String> specs) {
        this.specs = specs;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public RequestData getRequest() {
        return request;
    }

    public void setRequest(RequestData request) {
        this.request = request;
    }

    public ResponseData getExpectedResponse() {
        return expectedResponse;
    }

    public void setExpectedResponse(ResponseData expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public boolean meetsSpec(String specification) {
        return specs.contains(specification);
    }

    public boolean meetsOperationOfProduct(String operationName, String productName) {
        return operationName.equals(this.operationName) && productName.equals(this.productName);
    }

}
