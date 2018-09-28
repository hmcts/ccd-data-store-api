package uk.gov.hmcts.ccd.domain.service.security;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

public interface AuthorisedCaseDataFilter {

    void filterFields(CaseType caseType, CaseDetails caseDetails);

}
