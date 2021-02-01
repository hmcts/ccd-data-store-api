package uk.gov.hmcts.ccd.domain.service.message;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

@Builder
@Getter
public class MessageContext {

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetails;
    private String oldState;
}
