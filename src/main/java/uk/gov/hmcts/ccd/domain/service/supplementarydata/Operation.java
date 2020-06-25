package uk.gov.hmcts.ccd.domain.service.supplementarydata;

public enum Operation {
    INC("$inc"),
    SET("$set");

    private String operationName;

    Operation(String opName) {
        this.operationName = opName;
    }

    public String getOperationName() {
        return this.operationName;
    }
}
