package uk.gov.hmcts.ccd.domain.service.common;

import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Service
public class CaseEventEnablingService {

    private final EnablingConditionParser enablingConditionParser;

    @Inject
    public CaseEventEnablingService(EnablingConditionParser enablingConditionParser) {
        this.enablingConditionParser = enablingConditionParser;
    }

    public Boolean isEventEnabled(String enablingCondition,
                                  CaseDetails caseDetails) {
        if (StringUtils.isNotEmpty(enablingCondition)) {
            return this.enablingConditionParser.evaluate(enablingCondition,
                caseDetails.getCaseDataAndMetadata());
        }
        return Boolean.TRUE;
    }
}
