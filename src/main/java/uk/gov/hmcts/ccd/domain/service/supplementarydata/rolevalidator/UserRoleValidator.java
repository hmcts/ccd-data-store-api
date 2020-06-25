package uk.gov.hmcts.ccd.domain.service.supplementarydata.rolevalidator;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface UserRoleValidator {

    boolean canUpdateSupplementaryData(CaseDetails caseDetails);
}
