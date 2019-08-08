package uk.gov.hmcts.ccd.domain.service.createevent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;

@Getter
@AllArgsConstructor
@Builder(builderMethodName = "caseEventWith")
public class CreateCaseEventResult {

    private CaseEvent eventTrigger;
    private CaseDetails caseDetailsBefore;
    private CaseDetails savedCaseDetails;
}
