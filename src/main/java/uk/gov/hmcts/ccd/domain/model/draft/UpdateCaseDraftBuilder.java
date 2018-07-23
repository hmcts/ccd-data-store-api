package uk.gov.hmcts.ccd.domain.model.draft;

public class UpdateCaseDraftBuilder {
    private final UpdateCaseDraftRequest updateCaseDraftRequest;

    private UpdateCaseDraftBuilder() {
        this.updateCaseDraftRequest = new UpdateCaseDraftRequest();
    }

    public UpdateCaseDraftBuilder withDocument(CaseDraft document) {
        this.updateCaseDraftRequest.setDocument(document);
        return this;
    }

    public UpdateCaseDraftBuilder withType(String type) {
        this.updateCaseDraftRequest.setType(type);
        return this;
    }

    public static UpdateCaseDraftBuilder anUpdateCaseDraft() {
        return new UpdateCaseDraftBuilder();
    }

    public UpdateCaseDraftRequest build() {
        return this.updateCaseDraftRequest;
    }
}
