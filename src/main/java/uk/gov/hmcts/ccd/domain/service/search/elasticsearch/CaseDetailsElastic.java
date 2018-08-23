package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Map;

import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

public class CaseDetailsElastic {

    private String id;

    private String reference;

    private String jurisdiction;

    private String caseTypeId;

    private String createdDate;

    private String lastModified;

    private String state;

    private SecurityClassification securityClassification;

    private Map<String, Object> data;

    private Map<String, Object> dataClassification;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(Map<String, Object> dataClassification) {
        this.dataClassification = dataClassification;
    }
}
