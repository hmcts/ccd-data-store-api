package uk.gov.hmcts.ccd.data;

import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
