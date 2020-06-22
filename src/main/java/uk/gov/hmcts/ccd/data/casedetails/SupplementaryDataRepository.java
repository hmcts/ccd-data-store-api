package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface SupplementaryDataRepository {

    CaseDetails set(CaseDetails caseDetails);
}
