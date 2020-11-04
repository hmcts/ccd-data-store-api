package uk.gov.hmcts.ccd.domain.types;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public class ValidationContext {

    private final CaseDataContent currentCaseDataContent;
    private final String caseTypeId;
    private String path;

    public ValidationContext(CaseDataContent currentCaseDataContent, String caseTypeId) {
        this.currentCaseDataContent = currentCaseDataContent;
        this.caseTypeId = caseTypeId;
    }


    public CaseDataContent getCurrentCaseDataContent() {
        return currentCaseDataContent;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
