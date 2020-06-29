package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

public enum Operation {
    INC("$inc"),
    SET("$set"),
    FIND("$find");

    private String operationName;

    Operation(String opName) {
        this.operationName = opName;
    }

    public String getOperationName() {
        return this.operationName;
    }
}
