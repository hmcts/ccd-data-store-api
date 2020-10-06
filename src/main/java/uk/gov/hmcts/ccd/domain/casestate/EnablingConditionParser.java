package uk.gov.hmcts.ccd.domain.casestate;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface EnablingConditionParser {
    Boolean evaluate(String enablingCondition, Map<String, JsonNode> caseEventData);
}
