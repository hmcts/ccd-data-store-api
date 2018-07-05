package uk.gov.hmcts.ccd.domain.model.draft;

public class UpdateCaseDataContentDraftBuilder {
    private CaseDataContentDraft document;
    private String type;

    public UpdateCaseDataContentDraftBuilder withDocument(CaseDataContentDraft document) {
        this.document = document;
        return this;
    }

    public UpdateCaseDataContentDraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public static UpdateCaseDataContentDraftBuilder anUpdateCaseDraftBuilder() {
        return new UpdateCaseDataContentDraftBuilder();
    }

    public UpdateCaseDataContentDraft build() {
        return new UpdateCaseDataContentDraft(document, type);
    }
}
