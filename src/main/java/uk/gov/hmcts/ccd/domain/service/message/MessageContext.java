package uk.gov.hmcts.ccd.domain.service.message;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;

@Builder
@Getter
public class MessageContext {

    private CaseEventDefinition caseEventDefinition;
    private CaseDetails caseDetails;
    private String oldState;
}
