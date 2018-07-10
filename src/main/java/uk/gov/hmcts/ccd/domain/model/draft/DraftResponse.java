package uk.gov.hmcts.ccd.domain.model.draft;

import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDateTime;

@ToString
public class DraftResponse {

    private String id;

    private CaseDraft document;

    private String type;

    private LocalDateTime created;

    private LocalDateTime updated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CaseDraft getDocument() {
        return document;
    }

    public void setDocument(CaseDraft document) {
        this.document = document;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DraftResponse draft = (DraftResponse) o;

        return new EqualsBuilder()
            .append(id, draft.id)
            .append(document, draft.document)
            .append(type, draft.type)
            .append(created, draft.created)
            .append(updated, draft.updated)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(document)
            .append(type)
            .append(created)
            .append(updated)
            .toHashCode();
    }
}
