package uk.gov.hmcts.ccd.domain.model.draft;

public class UpdateCaseDraftBuilder {
    private CaseDraft document;
    private String type;

    public UpdateCaseDraftBuilder withDocument(CaseDraft document) {
        this.document = document;
        return this;
    }

    public UpdateCaseDraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public static UpdateCaseDraftBuilder anUpdateCaseDraftBuilder() {
        return new UpdateCaseDraftBuilder();
    }

    public UpdateCaseDraft build() {
        return new UpdateCaseDraft(document, type);
    }
}
