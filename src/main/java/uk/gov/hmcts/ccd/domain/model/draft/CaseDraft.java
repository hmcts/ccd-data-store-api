package uk.gov.hmcts.ccd.domain.model.draft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import lombok.ToString;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.Map;

@ToString
public class CaseDraft {

    private String userId;
    private String jurisdictionId;
    private String caseTypeId;
    private String eventId;

    private CaseDataContent caseDataContent;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public void setJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public CaseDataContent getCaseDataContent() {
        return caseDataContent;
    }

    public void setCaseDataContent(CaseDataContent caseDataContent) {
        this.caseDataContent = caseDataContent;
    }

    @JsonIgnore
    public SecurityClassification getSecurityClassification() {
        String securityClassification = caseDataContent != null ? caseDataContent.getSecurityClassification() : null;
        return securityClassification != null ? SecurityClassification.valueOf(caseDataContent.getSecurityClassification()) : null;
    }

    @JsonIgnore
    public Map<String, JsonNode> getData() {
        return caseDataContent != null ? caseDataContent.getData() : Maps.newHashMap();
    }

    @JsonIgnore
    public Map<String, JsonNode> getDataClassification() {
        return caseDataContent != null ? caseDataContent.getDataClassification() : Maps.newHashMap();
    }
}
