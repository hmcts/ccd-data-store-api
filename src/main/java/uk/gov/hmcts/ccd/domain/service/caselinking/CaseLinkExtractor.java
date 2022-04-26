package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseLinkExtractor {

    private static final String TEXT_CASE_REFERENCE = "TextCaseReference";

    private final CaseDataExtractor caseDataExtractor;

    @Autowired
    public CaseLinkExtractor(CaseDataExtractor caseDataExtractor) {
        this.caseDataExtractor = caseDataExtractor;
    }

    public List<String> getCaseLinks(Map<String, JsonNode> data, List<CaseFieldDefinition> caseFieldDefinitions) {
        return caseDataExtractor.extractFieldTypePaths(data, caseFieldDefinitions, TEXT_CASE_REFERENCE)
            .stream()
            .map(caseReference ->  JacksonUtils.getValueFromPath(caseReference, data))
            .collect(Collectors.toList());
    }
}
