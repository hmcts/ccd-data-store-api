package uk.gov.hmcts.ccd.domain.model.draft;

public class CreateCaseDraftBuilder {

    private final CreateCaseDraftRequest createCaseDraftRequest;

    private CreateCaseDraftBuilder() {
        this.createCaseDraftRequest = new CreateCaseDraftRequest();
    }

    public CreateCaseDraftBuilder withDocument(CaseDraft document) {
        this.createCaseDraftRequest.setDocument(document);
        return this;
    }

    public CreateCaseDraftBuilder withType(String type) {
        this.createCaseDraftRequest.setType(type);
        return this;
    }

    public CreateCaseDraftBuilder withMaxStaleDays(Integer maxStaleDays) {
        this.createCaseDraftRequest.setMaxStaleDays(maxStaleDays);
        return this;
    }

    public static CreateCaseDraftBuilder aCreateCaseDraft() {
        return new CreateCaseDraftBuilder();
    }

    public CreateCaseDraftRequest build() {
        return this.createCaseDraftRequest;
    }
}
