package uk.gov.hmcts.ccd.domain.model.draft;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public class CaseDraftBuilder {
    private String userId;
    private String jurisdictionId;
    private String caseTypeId;
    private String eventTriggerId;
    private CaseDataContent caseDataContent;

    public CaseDraftBuilder withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public CaseDraftBuilder withJurisdictionId(String jurisdictionId) {
        this.jurisdictionId = jurisdictionId;
        return this;
    }

    public CaseDraftBuilder withCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
        return this;
    }

    public CaseDraftBuilder withEventTriggerId(String eventTriggerId) {
        this.eventTriggerId = eventTriggerId;
        return this;
    }

    public CaseDraftBuilder withCaseDataContent(CaseDataContent caseDataContent) {
        this.caseDataContent = caseDataContent;
        return this;
    }

    public static CaseDraftBuilder aCaseDraft() {
        return new CaseDraftBuilder();
    }

    public CaseDataContentDraft build() {
        return new CaseDataContentDraft(userId, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
    }
}
