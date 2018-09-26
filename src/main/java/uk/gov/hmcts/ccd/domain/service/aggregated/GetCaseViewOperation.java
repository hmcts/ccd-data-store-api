package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;

public interface GetCaseViewOperation {

    /**
     *
     * @param jurisdictionId
     * @param caseTypeId
     * @param caseReference
     * @return When found, case for given reference, formatted for display
     * @deprecated Use {@link #execute(String)} instead
     */
    @Deprecated
    CaseView execute(String jurisdictionId, String caseTypeId, String caseReference);

    CaseView execute(String caseReference);
}
