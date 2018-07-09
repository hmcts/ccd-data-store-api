package uk.gov.hmcts.ccd.domain.model.draft;

import java.time.LocalDateTime;

public class DraftBuilder {
    private String id;
    private CaseDraft document;
    private String type;
    private LocalDateTime created;
    private LocalDateTime updated;

    public DraftBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public DraftBuilder withId(Long id) {
        this.id = String.valueOf(id);
        return this;
    }

    public DraftBuilder withDocument(CaseDraft document) {
        this.document = document;
        return this;
    }

    public DraftBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public DraftBuilder withCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public DraftBuilder withUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public static DraftBuilder aDraft() {
        return new DraftBuilder();
    }

    public Draft build() {
        return new Draft(id, document, type, created, updated);
    }
}
