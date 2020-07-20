package uk.gov.hmcts.ccd.domain.service.common;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface EndpointAuthorisationService {

    boolean isAccessAllowed(CaseDetails caseDetails);
}
