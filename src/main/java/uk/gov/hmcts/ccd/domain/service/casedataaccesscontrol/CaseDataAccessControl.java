package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;

public interface CaseDataAccessControl {
    Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails);

    void grantAccess(String caseId, String idamUserId);
}
