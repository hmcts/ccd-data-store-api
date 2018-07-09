package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.time.LocalDateTime;
import java.util.Map;

public class CaseDetailsBuilder {

    private final CaseDetails caseDetails;

    private CaseDetailsBuilder() {
        this.caseDetails = new CaseDetails();
    }

    public CaseDetailsBuilder withId(Long id) {
        this.caseDetails.setId(id);
        return this;
    }

    public CaseDetailsBuilder withReference(Long reference) {
        this.caseDetails.setReference(reference);
        return this;
    }

    public CaseDetailsBuilder withJurisdiction(String jurisdiction) {
        this.caseDetails.setJurisdiction(jurisdiction);
        return this;
    }

    public CaseDetailsBuilder withCaseTypeId(String caseTypeId) {
        this.caseDetails.setCaseTypeId(caseTypeId);
        return this;
    }

    public CaseDetailsBuilder withCreatedDate(LocalDateTime createdDate) {
        this.caseDetails.setCreatedDate(createdDate);
        return this;
    }

    public CaseDetailsBuilder withLastModified(LocalDateTime lastModified) {
        this.caseDetails.setLastModified(lastModified);
        return this;
    }

    public CaseDetailsBuilder withState(String state) {
        this.caseDetails.setState(state);
        return this;
    }

    public CaseDetailsBuilder withSecurityClassification(SecurityClassification securityClassification) {
        this.caseDetails.setSecurityClassification(securityClassification);
        return this;
    }

    public CaseDetailsBuilder withData(Map<String, JsonNode> data) {
        this.caseDetails.setData(data);
        return this;
    }

    public CaseDetailsBuilder withDataClassification(Map<String, JsonNode> dataClassification) {
        this.caseDetails.setDataClassification(dataClassification);
        return this;
    }

    public CaseDetails build() {
        return this.caseDetails;
    }
    public static CaseDetailsBuilder aCaseDetails() {
        return new CaseDetailsBuilder();
    }
}
