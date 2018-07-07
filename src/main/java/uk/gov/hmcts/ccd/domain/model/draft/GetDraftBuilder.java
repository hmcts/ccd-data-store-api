package uk.gov.hmcts.ccd.domain.model.draft;

import java.time.ZonedDateTime;

public class GetDraftBuilder {
    private String id;
    private String document;
    private String type;
    private ZonedDateTime created;
    private ZonedDateTime updated;

    public GetDraftBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public GetDraftBuilder withDocument(String document) {
        this.document = document;
        return this;
    }

    public GetDraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public GetDraftBuilder withCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public GetDraftBuilder withUpdated(ZonedDateTime updated) {
        this.updated = updated;
        return this;
    }

    public static GetDraftBuilder aGetDraft() {
        return new GetDraftBuilder();
    }

    public GetDraft build() {
        return new GetDraft(id, document, type, created, updated);
    }
}
