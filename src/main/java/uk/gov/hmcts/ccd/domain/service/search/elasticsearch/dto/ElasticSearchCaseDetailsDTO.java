package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ElasticSearchCaseDetailsDTO {

    private String id;
    private String reference;
    private String jurisdiction;
    private String caseTypeId;
    private LocalDateTime createdDate;
    private LocalDateTime lastModified;
    private LocalDateTime lastStateModifiedDate;
    private String state;
    private SecurityClassification securityClassification;
    private Map<String, JsonNode> data;
    private Map<String, JsonNode> dataClassification;

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

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
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

    public LocalDateTime getLastStateModifiedDate() {
        return lastStateModifiedDate;
    }

    public void setLastStateModifiedDate(LocalDateTime lastStateModifiedDate) {
        this.lastStateModifiedDate = lastStateModifiedDate;
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

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public Map<String, JsonNode> getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        this.dataClassification = dataClassification;
    }
}
