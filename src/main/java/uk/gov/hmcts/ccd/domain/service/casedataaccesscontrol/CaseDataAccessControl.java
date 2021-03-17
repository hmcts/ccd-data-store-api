package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import java.util.List;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

public interface CaseDataAccessControl {

    List<AccessProfile> applyAccessControl(RoleAssignmentFilteringResult filteringResults, CaseTypeDefinition caseTypeDefinition );

//        Optional<CaseDetails> applyAccessControl(CaseDetails caseDetails);

    void grantAccess(String caseId, String idamUserId);
}
