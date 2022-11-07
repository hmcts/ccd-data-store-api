package uk.gov.hmcts.ccd.domain.service.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionParser;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseEventEnablingService {

    private final EnablingConditionParser enablingConditionParser;

    private static final String INJECTED_DATA_DOT_DELIMITER = "[INJECTED_DATA.";
    private static final String INJECTED_DATA_DOT_DELIMITER_REGEX = "\\[INJECTED_DATA\\.";
    private static final String INJECTED_DATA_DOT_DELIMITER_REPLACEMENT = "[INJECTED_DATA__";

    @Inject
    public CaseEventEnablingService(EnablingConditionParser enablingConditionParser) {
        this.enablingConditionParser = enablingConditionParser;
    }

    public Boolean isEventEnabled(String enablingCondition,
                                  CaseDetails caseDetails,
                                  List<CaseViewField> callBackMetadata) {
        if (StringUtils.isNotEmpty(enablingCondition)) {

            if (enablingCondition.contains(INJECTED_DATA_DOT_DELIMITER)) {
                enablingCondition =
                    enablingCondition.replaceAll(INJECTED_DATA_DOT_DELIMITER_REGEX,
                        INJECTED_DATA_DOT_DELIMITER_REPLACEMENT);
            }
            Map<String, Object> mergedDataMap = new HashMap<>();
            mergedDataMap.putAll(caseDetails.getCaseDataAndMetadata());
            mergedDataMap.putAll(getCaseViewData(callBackMetadata));

            return this.enablingConditionParser.evaluate(enablingCondition, mergedDataMap);
        }
        return Boolean.TRUE;
    }

    private Map<String, Object> getCaseViewData(List<CaseViewField> caseViewFields) {
        return caseViewFields.stream()
            .filter(injectedDataCaseViewField ->
                injectedDataCaseViewField.getId().startsWith(INJECTED_DATA_DOT_DELIMITER))
            .collect(
                Collectors.toMap(
                    caseViewField -> caseViewField.getId()
                        .replace(INJECTED_DATA_DOT_DELIMITER, INJECTED_DATA_DOT_DELIMITER_REPLACEMENT),
                    CaseViewField::getValue));
    }
}
