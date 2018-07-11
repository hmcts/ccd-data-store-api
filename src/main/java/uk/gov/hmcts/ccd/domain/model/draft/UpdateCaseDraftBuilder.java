package uk.gov.hmcts.ccd.domain.model.draft;

public class UpdateCaseDraftBuilder {
    private final UpdateCaseDraft updateCaseDraft;

    private UpdateCaseDraftBuilder() {
        this.updateCaseDraft = new UpdateCaseDraft();
    }

    public UpdateCaseDraftBuilder withDocument(CaseDraft document) {
        this.updateCaseDraft.setDocument(document);
        return this;
    }

    public UpdateCaseDraftBuilder withType(String type) {
        this.updateCaseDraft.setType(type);
        return this;
    }

    public static UpdateCaseDraftBuilder anUpdateCaseDraft() {
        return new UpdateCaseDraftBuilder();
    }

    public UpdateCaseDraft build() {
        return this.updateCaseDraft;
    }
}
