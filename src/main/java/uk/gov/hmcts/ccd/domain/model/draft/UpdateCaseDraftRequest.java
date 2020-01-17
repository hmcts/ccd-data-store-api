package uk.gov.hmcts.ccd.domain.model.draft;

import javax.validation.constraints.NotNull;

public class UpdateCaseDraftRequest {

    private CaseDraft document;

    @NotNull
    private String type;

    public CaseDraft getDocument() {
        return document;
    }

    public String getType() {
        return type;
    }

    public void setDocument(CaseDraft document) {
        this.document = document;
    }

    public void setType(String type) {
        this.type = type;
    }
}
