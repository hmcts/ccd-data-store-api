package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

public class CaseDetailsElastic {

    private String id;

    private String reference;

    private String jurisdiction;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("last_modified")
    private LocalDateTime lastModified;

    private String state;

    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;

    @JsonProperty("data")
    private Map<String, Object> data;

    @JsonProperty("data_classification")
    private Map<String, Object> dataClassification;

    public CaseDetailsElastic() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
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
