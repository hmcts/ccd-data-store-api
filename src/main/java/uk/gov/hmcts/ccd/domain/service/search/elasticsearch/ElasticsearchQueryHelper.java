package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Slf4j
public class ElasticsearchQueryHelper {

    private static final String SORT = "sort";
    private static final String CASE_DATA_PREFIX = "data.";
    private static final String COLLECTION_VALUE_SUFFIX = ".value";
    private static final String KEYWORD_SUFFIX = ".keyword";

    private final ObjectMapper objectMapper;
    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;
    private final SearchQueryOperation searchQueryOperation;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final UserService userService;
    private final ElasticsearchMappings elasticsearchMappings;

    @Autowired
    public ElasticsearchQueryHelper(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                    ApplicationParams applicationParams,
                                    ObjectMapperService objectMapperService,
                                    SearchQueryOperation searchQueryOperation,
                                    @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) GetCaseTypeOperation getCaseTypeOperation,
                                    UserService userService,
                                    ElasticsearchMappings elasticsearchMappings) {
        this.objectMapper = objectMapper;
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
        this.searchQueryOperation = searchQueryOperation;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.userService = userService;
        this.elasticsearchMappings = elasticsearchMappings;
    }

    public CrossCaseTypeSearchRequest prepareRequest(List<String> caseTypeIds, String useCaseString, String jsonSearchRequest) {
        UseCase useCase;
        try {
            useCase = UseCase.valueOfReference(useCaseString);
        } catch (IllegalArgumentException ex) {
            throw new BadSearchRequest(String.format("The provided use case '%s' is unsupported.", useCaseString));
        }

        rejectBlackListedQuery(jsonSearchRequest);

        final List<String> updatedCaseTypeIds = buildCaseTypeIds(caseTypeIds);

        JsonNode searchRequest = stringToJsonNode(jsonSearchRequest);
        if (useCase != UseCase.DEFAULT) {
            applyConfiguredSort(searchRequest, updatedCaseTypeIds, useCase);
        }

        return new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(updatedCaseTypeIds)
            .withSearchRequest(searchRequest)
            .build();
    }

    private void applyConfiguredSort(JsonNode searchRequest, List<String> caseTypeIds, UseCase useCase) {
        JsonNode sortNode = searchRequest.get(SORT);
        if (sortNode == null) {
            ArrayNode appliedSortsNode = buildSortNode(caseTypeIds, useCase);
            if (appliedSortsNode.size() > 0) {
                ((ObjectNode)searchRequest).set(SORT, appliedSortsNode);
            }
        }
    }

    private ArrayNode buildSortNode(List<String> caseTypeIds, UseCase useCase) {
        ArrayNode sortNode = objectMapper.createArrayNode();
        caseTypeIds.forEach(caseTypeId -> addCaseTypeSorts(caseTypeId, useCase, sortNode));
        return sortNode;
    }

    private void addCaseTypeSorts(String caseTypeId, UseCase useCase, ArrayNode sortNode) {
        Optional<CaseTypeDefinition> caseTypeOpt = getCaseTypeOperation.execute(caseTypeId, CAN_READ);
        caseTypeOpt.ifPresent(caseType -> searchQueryOperation.getSortOrders(caseType, useCase)
            .forEach(field -> sortNode.add(buildSortOrderFieldNode(caseType, field))));
    }

    private ObjectNode buildSortOrderFieldNode(CaseTypeDefinition caseTypeDefinition, SortOrderField sortOrderField) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        final CommonField commonField = caseTypeDefinition.getCommonFieldByPath(sortOrderField.getCaseFieldId()).orElseThrow(() ->
            new NullPointerException(String.format("Case field '%s' does not exist in configuration for case type '%s'.",
                sortOrderField.getCaseFieldId(), caseTypeDefinition.getId()))
        );
        final FieldTypeDefinition fieldType = commonField.getFieldTypeDefinition();

        StringBuilder sb = new StringBuilder();

        if (sortOrderField.isMetadata()) {
            sb.append(MetaData.CaseField.valueOfReference(sortOrderField.getCaseFieldId()).getDbColumnName());
        } else {
            sb.append(CASE_DATA_PREFIX).append(sortOrderField.getCaseFieldId());
            if (fieldType.getType().equals(FieldTypeDefinition.COLLECTION)) {
                sb.append(COLLECTION_VALUE_SUFFIX);
            }
        }

        if ((sortOrderField.isMetadata() && elasticsearchMappings.isDefaultTextMetadata(sb.toString()))
             || (!sortOrderField.isMetadata() && elasticsearchMappings.isDefaultTextCaseData(fieldType))) {
            sb.append(KEYWORD_SUFFIX);
        }

        objectNode.set(sb.toString(), new TextNode(sortOrderField.getDirection()));
        return objectNode;
    }

    private JsonNode stringToJsonNode(String jsonSearchRequest) {
        return objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class);
    }

    private void rejectBlackListedQuery(String jsonSearchRequest) {
        List<String> blackListedQueries = applicationParams.getSearchBlackList();
        Optional<String> blackListedQueryOpt = blackListedQueries
            .stream()
            .filter(blacklisted -> {
                Pattern p = Pattern.compile("\\b" + blacklisted + "\\b");
                Matcher m = p.matcher(jsonSearchRequest);
                return m.find();
            })
            .findFirst();
        blackListedQueryOpt.ifPresent(blacklisted -> {
            throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
        });
    }

    private List<String> buildCaseTypeIds(List<String> caseTypeIds) {
        return CollectionUtils.isEmpty(caseTypeIds)
            ? userService.getUserCaseTypes().stream().map(CaseTypeDefinition::getId).collect(Collectors.toList())
            : caseTypeIds;
    }
}
