package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.CASE_DATA_PREFIX;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.COLLECTION_VALUE_SUFFIX;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;

@Service
@Slf4j
public class ElasticsearchSortService {

    private static final String KEYWORD_SUFFIX = ".keyword";

    private final ObjectMapper objectMapper;
    private final SearchQueryOperation searchQueryOperation;
    private final CaseTypeService caseTypeService;
    private final ElasticsearchMappings elasticsearchMappings;

    @Autowired
    public ElasticsearchSortService(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                    SearchQueryOperation searchQueryOperation,
                                    CaseTypeService caseTypeService,
                                    ElasticsearchMappings elasticsearchMappings) {
        this.objectMapper = objectMapper;
        this.searchQueryOperation = searchQueryOperation;
        this.caseTypeService = caseTypeService;
        this.elasticsearchMappings = elasticsearchMappings;
    }

    public void applyConfiguredSort(ElasticsearchRequest searchRequest, String caseTypeId, String useCase) {
        ArrayNode appliedSortsNode = buildSortNode(searchRequest, caseTypeId, useCase);
        searchRequest.setSort(appliedSortsNode);
    }

    private ArrayNode buildSortNode(ElasticsearchRequest searchRequest, String caseTypeId, String useCase) {
        ArrayNode sortNode = searchRequest.isSorted() && searchRequest.getSort().isArray()
            ? (ArrayNode) searchRequest.getSort()
            : objectMapper.createArrayNode();

        if (sortNode.isEmpty() && useCase != null) {
            addCaseTypeSorts(caseTypeId, useCase, sortNode);
        }

        // Always add created_date as the final sort (ES defaults to ascending when direction is not specified)
        sortNode.add(new TextNode(CREATED_DATE.getDbColumnName()));
        return sortNode;
    }

    private void addCaseTypeSorts(String caseTypeId, String useCase, ArrayNode sortNode) {
        CaseTypeDefinition caseType = caseTypeService.getCaseType(caseTypeId);
        searchQueryOperation.getSortOrders(caseType, useCase).forEach(field -> sortNode.add(buildSortOrderFieldNode(caseType, field)));
    }

    private ObjectNode buildSortOrderFieldNode(CaseTypeDefinition caseTypeDefinition, SortOrderField sortOrderField) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        CommonField commonField = caseTypeDefinition.getComplexSubfieldDefinitionByPath(sortOrderField.getCaseFieldId()).orElseThrow(() ->
            new ServiceException(String.format("Case field '%s' does not exist in configuration for case type '%s'.",
                sortOrderField.getCaseFieldId(), caseTypeDefinition.getId()))
        );
        FieldTypeDefinition fieldType = commonField.getFieldTypeDefinition();

        StringBuilder sb = new StringBuilder();

        if (sortOrderField.isMetadata()) {
            sb.append(MetaData.CaseField.valueOfReference(sortOrderField.getCaseFieldId()).getDbColumnName());
        } else {
            sb.append(CASE_DATA_PREFIX).append(sortOrderField.getCaseFieldId());
            if (fieldType.getType().equals(FieldTypeDefinition.COLLECTION)) {
                sb.append(COLLECTION_VALUE_SUFFIX);
            }
        }

        // Fields mapped in ElasticSearch as "defaultText" require the "keyword" suffix to sort
        if ((sortOrderField.isMetadata() && elasticsearchMappings.isDefaultTextMetadata(sb.toString()))
             || (!sortOrderField.isMetadata() && elasticsearchMappings.isDefaultTextCaseData(fieldType))) {
            sb.append(KEYWORD_SUFFIX);
        }

        objectNode.set(sb.toString(), new TextNode(sortOrderField.getDirection()));
        return objectNode;
    }
}
