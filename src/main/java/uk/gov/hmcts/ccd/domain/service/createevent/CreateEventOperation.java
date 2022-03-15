package uk.gov.hmcts.ccd.domain.service.createevent;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface CreateEventOperation {

    CaseDetails createCaseEvent(String caseReference, CaseDataContent caseDataContent);

    CaseDetails createCaseSystemEvent(String caseReference,
                                      Integer version,
                                      String attributePath,
                                      String categoryId);

}
