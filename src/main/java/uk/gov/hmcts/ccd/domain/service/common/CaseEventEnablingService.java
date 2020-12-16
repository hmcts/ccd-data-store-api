package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;

@Service
public class CaseEventEnablingService {

    private final EnablingConditionParser enablingConditionParser;

    @Inject
    public CaseEventEnablingService(EnablingConditionParser enablingConditionParser) {
        this.enablingConditionParser = enablingConditionParser;
    }

    public Boolean evaluate(String enablingCondition,
                           Map<String, JsonNode> caseEventData) {
        if (StringUtils.isNotEmpty(enablingCondition)) {
            return this.enablingConditionParser.evaluate(enablingCondition, caseEventData);
        }
        return Boolean.TRUE;
    }
}
