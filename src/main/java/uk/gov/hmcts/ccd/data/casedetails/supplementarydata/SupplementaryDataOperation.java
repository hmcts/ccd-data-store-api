package uk.gov.hmcts.ccd.data.casedetails.supplementarydata;

import java.util.Arrays;
import java.util.Optional;

public enum SupplementaryDataOperation {
    INC("$inc"),
    SET("$set"),
    FIND("$find");

    private String operationName;

    SupplementaryDataOperation(String opName) {
        this.operationName = opName;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public static Optional<SupplementaryDataOperation> getOperation(String operationName) {
        return Arrays.stream(values()).filter(operation ->
            operation
                .getOperationName()
                .equalsIgnoreCase(operationName))
            .findFirst();
    }
}
