package uk.gov.hmcts.ccd.data.caseaccess;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "case_link")
public class CaseLinkEntity {

    public static class CaseLinkPrimaryKey implements Serializable {
        @Column(name = "case_id", nullable = false)
        private Long caseId;
        @Column(name = "linked_case_id", nullable = false)
        private Long linkedCaseId;

        public long getCaseId() {
            return caseId;
        }

        public void setCaseId(long caseId) {
            this.caseId = caseId;
        }

        public long getLinkedCaseId() {
            return linkedCaseId;
        }

        public void setLinkedCaseId(long linkedCaseId) {
            this.linkedCaseId = linkedCaseId;
        }
    }

    @EmbeddedId
    private CaseLinkPrimaryKey caseLinkPrimaryKey;

    @Column(name = "case_type_id", nullable = false)
    private String caseTypeId;

    public CaseLinkEntity() {

    }

    public CaseLinkEntity(Long caseId, Long linkedCaseId, String caseTypeId) {
        caseLinkPrimaryKey = new CaseLinkPrimaryKey();
        caseLinkPrimaryKey.setCaseId(caseId);
        caseLinkPrimaryKey.setLinkedCaseId(linkedCaseId);

        this.caseTypeId = caseTypeId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public CaseLinkPrimaryKey getCaseLinkPrimaryKey() {
        return caseLinkPrimaryKey;
    }
}
