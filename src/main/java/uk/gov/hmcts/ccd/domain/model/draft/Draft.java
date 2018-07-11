package uk.gov.hmcts.ccd.domain.model.draft;

import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.time.LocalDateTime;

@ToString
public class Draft {

    private Long id;

    private CaseDataContent document;

    private String type;

    private LocalDateTime created;

    private LocalDateTime updated;

    public Draft(Long id, CaseDataContent document, String type, LocalDateTime created, LocalDateTime updated) {
        this.id = id;
        this.document = document;
        this.type = type;
        this.created = created;
        this.updated = updated;
    }

    public Draft() {
    /*
        Jackson required
     */
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CaseDataContent getDocument() {
        return document;
    }

    public void setDocument(CaseDataContent document) {
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

        Draft draft = (Draft) o;

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
