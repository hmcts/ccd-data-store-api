package uk.gov.hmcts.ccd.domain.model.draft;

public class CreateCaseDraftBuilder {
    private CaseDraft document;
    private String type;
    private Integer maxStaleDays;

    public CreateCaseDraftBuilder withDocument(CaseDraft document) {
        this.document = document;
        return this;
    }

    public CreateCaseDraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public CreateCaseDraftBuilder withMaxStaleDays(Integer maxStaleDays) {
        this.maxStaleDays = maxStaleDays;
        return this;
    }

    public static CreateCaseDraftBuilder aCreateCaseDraft() {
        return new CreateCaseDraftBuilder();
    }

    public CreateCaseDraft build() {
        return new CreateCaseDraft(document, type, maxStaleDays);
    }
}
