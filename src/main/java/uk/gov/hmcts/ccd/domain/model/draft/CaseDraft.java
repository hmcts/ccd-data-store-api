package uk.gov.hmcts.ccd.domain.model.draft;

import lombok.ToString;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

@ToString
public class CaseDraft {

    private String userId;
    private String jurisdictionId;
    private String caseTypeId;
    private String eventTriggerId;
    private CaseDataContent caseDataContent;

    public CaseDraft() {
    }

    public CaseDraft(String userId, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        this.userId = userId;
        this.jurisdictionId = jurisdictionId;
        this.caseTypeId = caseTypeId;
        this.eventTriggerId = eventTriggerId;
        this.caseDataContent = caseDataContent;
    }

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

    public String getEventTriggerId() {
        return eventTriggerId;
    }

    public void setEventTriggerId(String eventTriggerId) {
        this.eventTriggerId = eventTriggerId;
    }

    public CaseDataContent getCaseDataContent() {
        return caseDataContent;
    }

    public void setCaseDataContent(CaseDataContent caseDataContent) {
        this.caseDataContent = caseDataContent;
    }
}
