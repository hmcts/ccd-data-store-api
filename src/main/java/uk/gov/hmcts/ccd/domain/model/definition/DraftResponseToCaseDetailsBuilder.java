package uk.gov.hmcts.ccd.domain.model.definition;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.Objects;

import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetails.DRAFT_ID;

@Named
@Singleton
public class DraftResponseToCaseDetailsBuilder {

    public CaseDetails build(DraftResponse draftResponse) {
        CaseDraft document = Objects.requireNonNull(draftResponse, "draftResponse must not be null").getDocument();
        Objects.requireNonNull(document, "draft document must not be null");
        final CaseDetails newCaseDetails = new CaseDetails();
        newCaseDetails.setId(String.format(DRAFT_ID, draftResponse.getId()));
        newCaseDetails.setCaseTypeId(document.getCaseTypeId());
        newCaseDetails.setJurisdiction(document.getJurisdictionId());
        newCaseDetails.setSecurityClassification(document.getSecurityClassification());
        newCaseDetails.setDataClassification(document.getDataClassification());
        newCaseDetails.setData(Objects.requireNonNull(document.getCaseDataContent(),
            "case data content must not be null").getData());
        newCaseDetails.setCreatedDate(draftResponse.getCreated());
        newCaseDetails.setLastModified(draftResponse.getUpdated());
        return newCaseDetails;
    }

}
