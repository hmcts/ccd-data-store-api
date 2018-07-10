package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public class DraftBuilder {
    private Draft draft;

    public DraftBuilder() {
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

    public DraftBuilder withCreated(LocalDateTime created) {
        this.draft.setCreated(created);
        return this;
    }

    public DraftBuilder withUpdated(LocalDateTime updated) {
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
