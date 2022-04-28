package uk.gov.hmcts.ccd.domain.service.caselinking;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.caselinking.CaseLink;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataExtractor;
import uk.gov.hmcts.ccd.domain.types.CustomTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseLinkExtractor {

    protected static final String STANDARD_CASE_LINK_FIELD = "caseLinks";
    protected static final String TEXT_CASE_REFERENCE = CustomTypes.CASE_LINK_TEXT_CASE_REFERENCE.getId();

    private final CaseDataExtractor caseDataExtractor;

    public CaseLinkExtractor(CaseDataExtractor caseDataExtractor) {
        this.caseDataExtractor = caseDataExtractor;
    }

    public List<CaseLink> getCaseLinksFromData(CaseDetails caseDetails,
                                               List<CaseFieldDefinition> caseFieldDefinitions) {
        Map<String, JsonNode> data = caseDetails.getData();

        // extract all the paths to CaseLink fields and then processes the paths back into the raw data
        List<String> allLinkedCaseReferences =
            caseDataExtractor.extractFieldTypePaths(data, caseFieldDefinitions, TEXT_CASE_REFERENCE)
                .stream()
                .map(metadata -> JacksonUtils.getValueFromPath(metadata.getPath(), data))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // look for Standard CaseLink field and find all caseReferences present
        List<String> standardCaseReferences = new ArrayList<>();

        if (isStandardCaseLinkFieldInUse(caseFieldDefinitions)) {
            JsonNode jsonNode = data.get(STANDARD_CASE_LINK_FIELD);
            if (jsonNode != null && jsonNode.isArray()) {
                for (JsonNode caseLinkNode : jsonNode) {
                    JsonNode nestedCaseFieldByPath =
                        CaseFieldPathUtils.getNestedCaseFieldByPath(caseLinkNode, "value.CaseReference");
                    standardCaseReferences.add(nestedCaseFieldByPath.textValue());
                }
            }
        }

        return allLinkedCaseReferences.stream()
            .map(linkedCaseReference -> CaseLink.builder()
                .caseReference(caseDetails.getReference())
                .caseTypeId(caseDetails.getCaseTypeId())
                .linkedCaseReference(Long.parseLong(linkedCaseReference))
                // set Standard Case Link flag if value also present on list of standard case links
                .standardLink(standardCaseReferences.stream().anyMatch(s -> s.equals(linkedCaseReference)))
                .build())
            .collect(Collectors.toList());
    }

    private boolean isStandardCaseLinkFieldInUse(List<CaseFieldDefinition> caseFieldDefinitions) {
        return caseFieldDefinitions
            .stream()
            .anyMatch(key -> key.getId().equals(STANDARD_CASE_LINK_FIELD)
                && key.isCollectionFieldType()
                && key.getFieldTypeDefinition().getCollectionFieldTypeDefinition() != null
                && FieldTypeDefinition.PREDEFINED_COMPLEX_CASELINK
                    .equals(key.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getId()));
    }

}
