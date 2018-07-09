package uk.gov.hmcts.ccd.domain.model.draft;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public class CaseDraftBuilder {
    private CaseDraft caseDraft;

    private CaseDraftBuilder() {
        this.caseDraft = new CaseDraft();
    }

    public CaseDraftBuilder withUserId(String userId) {
        this.caseDraft.setUserId(userId);
        return this;
    }

    public CaseDraftBuilder withJurisdictionId(String jurisdictionId) {
        this.caseDraft.setJurisdictionId(jurisdictionId);
        return this;
    }

    public CaseDraftBuilder withCaseTypeId(String caseTypeId) {
        this.caseDraft.setCaseTypeId(caseTypeId);
        return this;
    }

    public CaseDraftBuilder withEventTriggerId(String eventTriggerId) {
        this.caseDraft.setEventTriggerId(eventTriggerId);
        return this;
    }

    public CaseDraftBuilder withCaseDataContent(CaseDataContent caseDataContent) {
        this.caseDraft.setCaseDataContent(caseDataContent);
        return this;
    }

    public static CaseDraftBuilder aCaseDraft() {
        return new CaseDraftBuilder();
    }

    public CaseDraft build() {
        return this.caseDraft;
    }
}
