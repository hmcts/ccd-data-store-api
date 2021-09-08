package uk.gov.hmcts.ccd.domain.model.draft;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

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

    public DraftResponse() {
    }

    public DraftResponse(String id) {
        this.id = id;
    }

    @JsonIgnore
    public String getCaseTypeId() {
        return getDocument() != null ? getDocument().getCaseTypeId() : null;
    }

    @JsonIgnore
    public CaseDataContent getCaseDataContent() {
        return getDocument() != null ? getDocument().getCaseDataContent() : null;
    }
}
