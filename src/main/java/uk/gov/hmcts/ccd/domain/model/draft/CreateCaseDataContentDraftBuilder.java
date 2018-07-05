package uk.gov.hmcts.ccd.domain.model.draft;

public class CreateCaseDataContentDraftBuilder {
    private CaseDataContentDraft document;
    private String type;
    private Integer maxStaleDays;

    public CreateCaseDataContentDraftBuilder withDocument(CaseDataContentDraft document) {
        this.document = document;
        return this;
    }

    public CreateCaseDataContentDraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public CreateCaseDataContentDraftBuilder withMaxStaleDays(Integer maxStaleDays) {
        this.maxStaleDays = maxStaleDays;
        return this;
    }

    public static CreateCaseDataContentDraftBuilder aCreateCaseDraftBuilder() {
        return new CreateCaseDataContentDraftBuilder();
    }

    public CreateCaseDataContentDraft build() {
        return new CreateCaseDataContentDraft(document, type, maxStaleDays);
    }
}
