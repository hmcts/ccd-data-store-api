package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.ZonedDateTime;

public class DraftBuilder {
    private final Draft draft;

    private DraftBuilder() {
        draft = new Draft();
    }

    public DraftBuilder withId(String id) {
        this.draft.setId(id);
        return this;
    }

    public DraftBuilder withId(Long id) {
        this.draft.setId(String.valueOf(id));
        return this;
    }

    public DraftBuilder withDocument(JsonNode document) {
        this.draft.setDocument(document);
        return this;
    }

    public DraftBuilder withType(String type) {
        this.draft.setType(type);
        return this;
    }

    public DraftBuilder withCreated(ZonedDateTime created) {
        this.draft.setCreated(created);
        return this;
    }

    public DraftBuilder withUpdated(ZonedDateTime updated) {
        this.draft.setUpdated(updated);
        return this;
    }

    public static DraftBuilder aDraft() {
        return new DraftBuilder();
    }

    public Draft build() {
        return this.draft;
    }
}
