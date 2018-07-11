package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

import java.time.ZonedDateTime;

@ToString
public class Draft {

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

}
