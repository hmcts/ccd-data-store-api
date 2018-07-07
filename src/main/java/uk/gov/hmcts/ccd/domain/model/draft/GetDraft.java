package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.ZonedDateTime;

public class GetDraft {

    private String id;

    @JsonRawValue
    private String document;

    private String type;

    private ZonedDateTime created;

    private ZonedDateTime updated;

    public GetDraft() {
        // Default constructor for JSON mapper
    }

    // region constructor
    public GetDraft(
        String id,
        String document,
        String type,
        ZonedDateTime created,
        ZonedDateTime updated
    ) {
        this.id = id;
        this.document = document;
        this.type = type;
        this.created = created;
        this.updated = updated;
    }
    // endregion


    public String getId() {
        return id;
    }

    public String getDocument() {
        return document;
    }

    public String getType() {
        return type;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }
}
