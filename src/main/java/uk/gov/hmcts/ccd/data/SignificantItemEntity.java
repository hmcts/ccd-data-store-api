package uk.gov.hmcts.ccd.data;

import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.domain.model.callbacks.ItemType;
import javax.persistence.*;
import java.net.URL;
@Table(name = "case_event_significant_items")
@Entity
public class SignificantItemEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemType type;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "url",  nullable = false)
    private URL url;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_event_id")
    private CaseAuditEventEntity caseEvent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public CaseAuditEventEntity getCaseEvent() {
        return caseEvent;
    }

    public void setCaseEvent(CaseAuditEventEntity caseEvent) {
        this.caseEvent = caseEvent;
    }

}
