package uk.gov.hmcts.ccd.data.caselinking;

import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@EqualsAndHashCode
@Table(name = "case_link")
public class CaseLinkEntity {

    public static final Boolean STANDARD_LINK = true;
    public static final Boolean NON_STANDARD_LINK = false;

    @EqualsAndHashCode
    public static class CaseLinkPrimaryKey implements Serializable {
        @Column(name = "case_id", nullable = false)
        private Long caseId;
        @Column(name = "linked_case_id", nullable = false)
        private Long linkedCaseId;

        public Long getCaseId() {
            return caseId;
        }

        public void setCaseId(long caseId) {
            this.caseId = caseId;
        }

        public Long getLinkedCaseId() {
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

    @Column(name = "standard_link", nullable = false)
    private Boolean standardLink;

    public CaseLinkEntity() {

    }

    public CaseLinkEntity(Long caseId, Long linkedCaseId, String caseTypeId, Boolean standardLink) {
        caseLinkPrimaryKey = new CaseLinkPrimaryKey();
        caseLinkPrimaryKey.setCaseId(caseId);
        caseLinkPrimaryKey.setLinkedCaseId(linkedCaseId);

        this.caseTypeId = caseTypeId;
        this.standardLink = standardLink;
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

    public Boolean getStandardLink() {
        return standardLink;
    }

    public void setStandardLink(Boolean standardLink) {
        this.standardLink = standardLink;
    }

}
