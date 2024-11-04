package uk.gov.hmcts.ccd.data;

import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Table(name = "case_event_significant_items")
@Entity
public class SignificantItemEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SignificantItemType type;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "url",  nullable = false)
    private String url;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_event_id")
    private CaseAuditEventEntity caseEvent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SignificantItemType getType() {
        return type;
    }

    public void setType(SignificantItemType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public CaseAuditEventEntity getCaseEvent() {
        return caseEvent;
    }

    public void setCaseEvent(CaseAuditEventEntity caseEvent) {
        this.caseEvent = caseEvent;
    }

}
