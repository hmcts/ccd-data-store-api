package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataExtractor;
import uk.gov.hmcts.ccd.domain.types.CustomTypes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseLinkExtractor {

    protected static final String TEXT_CASE_REFERENCE = CustomTypes.CASE_LINK_TEXT_CASE_REFERENCE.getId();

    private final CaseDataExtractor caseDataExtractor;

    @Autowired
    public CaseLinkExtractor(CaseDataExtractor caseDataExtractor) {
        this.caseDataExtractor = caseDataExtractor;
    }

    public List<String> getCaseLinksFromData(CaseDetails caseDetails,
                                             List<CaseFieldDefinition> caseFieldDefinitions) {
        Map<String, JsonNode> data = caseDetails.getData();

        // extract all the paths to CaseLink fields and then processes the paths back into the raw data
        return caseDataExtractor.extractFieldTypePaths(data, caseFieldDefinitions, TEXT_CASE_REFERENCE)
            .stream()
            .map(caseReference ->  JacksonUtils.getValueFromPath(caseReference, data))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }
}
