package uk.gov.hmcts.ccd.domain.enablingcondition;

import java.util.Map;

public interface EnablingConditionParser {
    Boolean evaluate(String enablingCondition, Map<String, ?> caseEventData);
}
