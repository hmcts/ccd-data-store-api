package uk.gov.hmcts.ccd.domain.service.createcase;

import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface CreateCaseOperation {
    CaseDetails createCaseDetails(String caseTypeId,
                                  CaseDataContent content,
                                  Boolean ignoreWarning);
}
