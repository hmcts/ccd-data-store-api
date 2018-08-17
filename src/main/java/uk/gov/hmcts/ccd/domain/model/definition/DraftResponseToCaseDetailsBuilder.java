package uk.gov.hmcts.ccd.domain.model.definition;

import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import javax.inject.Named;
import javax.inject.Singleton;

import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetails.DRAFT_ID;

@Named
@Singleton
public class DraftResponseToCaseDetailsBuilder {

    public CaseDetails build(DraftResponse draftResponse) {
        CaseDraft document = draftResponse.getDocument();
        final CaseDetails newCaseDetails = new CaseDetails();
        newCaseDetails.setId(String.format(DRAFT_ID, draftResponse.getId()));
        newCaseDetails.setCaseTypeId(document.getCaseTypeId());
        newCaseDetails.setJurisdiction(document.getJurisdictionId());
        newCaseDetails.setData(document.getCaseDataContent().getData());
        return newCaseDetails;
    }
}
