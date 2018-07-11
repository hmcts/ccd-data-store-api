package uk.gov.hmcts.ccd.domain.model.draft;

import java.time.LocalDateTime;

public class DraftResponseBuilder {
    private final DraftResponse draftResponse;

    private DraftResponseBuilder() {
        draftResponse = new DraftResponse();
    }

    public DraftResponseBuilder withId(String id) {
        this.draftResponse.setId(id);
        return this;
    }

    public DraftResponseBuilder withId(Long id) {
        this.draftResponse.setId(String.valueOf(id));
        return this;
    }

    public DraftResponseBuilder withDocument(CaseDraft document) {
        this.draftResponse.setDocument(document);
        return this;
    }

    public DraftResponseBuilder withType(String type) {
        this.draftResponse.setType(type);
        return this;
    }

    public DraftResponseBuilder withCreated(LocalDateTime created) {
        this.draftResponse.setCreated(created);
        return this;
    }

    public DraftResponseBuilder withUpdated(LocalDateTime updated) {
        this.draftResponse.setUpdated(updated);
        return this;
    }

    public static DraftResponseBuilder aDraftResponse() {
        return new DraftResponseBuilder();
    }

    public DraftResponse build() {
        return this.draftResponse;
    }
}
