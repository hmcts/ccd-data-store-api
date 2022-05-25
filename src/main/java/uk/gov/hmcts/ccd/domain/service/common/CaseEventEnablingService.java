package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseEventEnablingService {

    private final EnablingConditionParser enablingConditionParser;

    private static final String DOT_DELIMITER = ".";
    private static final String DOT_DELIMITER_REPLACEMENT = "__";

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

    public Boolean isEventEnabled(String enablingCondition,
                                  List<CaseViewField> caseViewFields) {
        if (StringUtils.isNotEmpty(enablingCondition)) {
            Map<String, Object> caseViewFieldData = caseViewFields.stream()
                .filter(injectedDataCaseViewField -> injectedDataCaseViewField.getId().startsWith("[INJECTED_DATA"))
                .collect(
                    Collectors.toMap(
                        caseViewField -> replaceDotDelimiter(caseViewField.getId()),
                        caseViewField -> TextNode.valueOf((String) caseViewField.getValue())));

            return !caseViewFieldData.isEmpty()
                && this.enablingConditionParser.evaluate(replaceDotDelimiter(enablingCondition), caseViewFieldData);
        }
        return Boolean.TRUE;
    }

    private String replaceDotDelimiter(String delimitedString) {
        return delimitedString.replace(DOT_DELIMITER, DOT_DELIMITER_REPLACEMENT);
    }
}
