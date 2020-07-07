package uk.gov.hmcts.ccd.domain.model.draft;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

@ToString
public class Draft {
    public static final String DRAFT = "DRAFT";

    private String id;

    private JsonNode document;

    private String type;

    private ZonedDateTime created;

    private ZonedDateTime updated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonNode getDocument() {
        return document;
    }

    public void setDocument(JsonNode document) {
        this.document = document;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(ZonedDateTime updated) {
        this.updated = updated;
    }

    public static String stripId(String draftId) {
        if (draftId.startsWith(DRAFT)) {
            return draftId.substring(DRAFT.length());
        } else {
            return draftId;
        }
    }

    public static boolean isDraft(String caseReference) {
        return caseReference.startsWith(DRAFT);
    }
}
