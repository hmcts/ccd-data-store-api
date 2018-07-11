package uk.gov.hmcts.ccd.domain.model.draft;

public class CreateCaseDraftBuilder {

    private final CreateCaseDraft createCaseDraft;

    private CreateCaseDraftBuilder() {
        this.createCaseDraft = new CreateCaseDraft();
    }

    public CreateCaseDraftBuilder withDocument(CaseDraft document) {
        this.createCaseDraft.setDocument(document);
        return this;
    }

    public CreateCaseDraftBuilder withType(String type) {
        this.createCaseDraft.setType(type);
        return this;
    }

    public CreateCaseDraftBuilder withMaxStaleDays(Integer maxStaleDays) {
        this.createCaseDraft.setMaxStaleDays(maxStaleDays);
        return this;
    }

    public static CreateCaseDraftBuilder aCreateCaseDraft() {
        return new CreateCaseDraftBuilder();
    }

    public CreateCaseDraft build() {
        return this.createCaseDraft;
    }
}
