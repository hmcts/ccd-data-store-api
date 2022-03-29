package uk.gov.hmcts.ccd.domain.service.casedeletion;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.casedeletion.CaseLink;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseLinkExtractor {

    private static final String TEXT_CASE_REFERENCE = "TextCaseReference";
    private static final String STANDARD_CASE_LINK_FIELD = "caseLinks";

    private final CaseDataExtractor caseDataExtractor;

    @Autowired
    public CaseLinkExtractor(CaseDataExtractor caseDataExtractor) {
        this.caseDataExtractor = caseDataExtractor;
    }

    /**
     * extracts all of the paths and then processes the paths back into the raw data
     */
    public List<CaseLink> getCaseLinksFromData(Map<String, JsonNode> data, List<CaseFieldDefinition> caseFieldDefinitions) {
        List<String> linkedCaseRefs = caseDataExtractor.extractFieldTypePaths(data, caseFieldDefinitions, TEXT_CASE_REFERENCE)
            .stream()
            .map(caseReference -> JacksonUtils.getValueFromPath(caseReference, data))
            .filter(caseLinkString -> caseLinkString != null && !caseLinkString.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        List<String> standardCaseReferences = Collections.emptyList();

        if (isCaseLinkTopLevelCollection(caseFieldDefinitions)) {
            JsonNode jsonNode = data.get(STANDARD_CASE_LINK_FIELD);
            if(jsonNode != null && jsonNode.isArray()) {
                for (JsonNode caseLinkNode : jsonNode) {
                    JsonNode nestedCaseFieldByPath = CaseFieldPathUtils.getNestedCaseFieldByPath(caseLinkNode, "value.CaseReference");
                    standardCaseReferences.add(nestedCaseFieldByPath.textValue());
                }
            }
        }

//        linkedCaseRefs.stream()
//            .anyMatch(standardCaseReferences::contains);

//        List<CaseLink> finalCaseLinks = new ArrayList<>();
//        for (String caseRef : linkedCaseRefs) {
//            finalCaseLinks.add(CaseLink.builder().caseReference(Long.valueOf(caseRef)).standard_link(true).build());
//        }

        return linkedCaseRefs.stream()
            .filter(two -> standardCaseReferences.stream()
                .anyMatch(one -> one.equals(two)))
            .map(caseLink -> CaseLink.builder()
                .linkedCaseReference(Long.parseLong(caseLink))
                .standard_link(true).build())
            .collect(Collectors.toList());
    }

    // check if standard case links field is in use
    private boolean isCaseLinkTopLevelCollection(List<CaseFieldDefinition> caseFieldDefinitions){
        return caseFieldDefinitions
            .stream()
            .anyMatch(key -> key.getId().equals(STANDARD_CASE_LINK_FIELD)
                && key.isCollectionFieldType()
                && key.getFieldTypeDefinition().getCollectionFieldTypeDefinition() != null
                && key.getFieldTypeDefinition()
                .getCollectionFieldTypeDefinition()
                .getId().equals(FieldTypeDefinition.PREDEFINED_COMPLEX_CASELINK));
    }
}
