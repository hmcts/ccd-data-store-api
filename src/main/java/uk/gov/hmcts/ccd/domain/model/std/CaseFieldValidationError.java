package uk.gov.hmcts.ccd.domain.model.std;

import java.io.Serializable;

public class CaseFieldValidationError implements Serializable {
    private final String id;
    private final String message;

    public CaseFieldValidationError(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }
}
