package uk.gov.hmcts.ccd.domain.service.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.*;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.SearchResultProcessor;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;

@Service
public class CaseSearchResultViewGenerator {

    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;
    private final SearchQueryOperation searchQueryOperation;
    private final SearchResultProcessor searchResultProcessor;

    public CaseSearchResultViewGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                         CaseTypeService caseTypeService,
                                         SearchQueryOperation searchQueryOperation,
                                         SearchResultProcessor searchResultProcessor) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
        this.searchQueryOperation = searchQueryOperation;
        this.searchResultProcessor = searchResultProcessor;
    }

    public CaseSearchResultView execute(String caseTypeId,
                                        CaseSearchResult caseSearchResult,
                                        String useCase) {
        List<SearchResultViewHeaderGroup> headerGroups = buildHeaders(caseTypeId, useCase, caseSearchResult);
        List<SearchResultViewItem> items = buildItems(useCase, caseSearchResult, caseTypeId);

        if (itemsRequireFormatting(headerGroups)) {
            items = searchResultProcessor.execute(headerGroups.get(0).getFields(), items);
        }

        // TODO: Filter out fields from result that haven't been requested before returning (RDM-8556)
        return new CaseSearchResultView(
            headerGroups,
            items,
            caseSearchResult.getTotal()
        );
    }

    private boolean itemsRequireFormatting(List<SearchResultViewHeaderGroup> headerGroups) {
        return headerGroups.size() == 1
               && headerGroups.get(0).getFields().stream().anyMatch(field -> field.getDisplayContextParameter() != null);
    }

    private List<SearchResultViewItem> buildItems(String useCase, CaseSearchResult caseSearchResult, String caseTypeId) {
        CaseTypeDefinition caseTypeDefinition = getCaseTypeDefinition(caseTypeId);
        SearchResult searchResultDefinition = searchQueryOperation.getSearchResultDefinition(caseTypeDefinition, useCase);

        List<SearchResultViewItem> items = new ArrayList<>();
        // Only one case type is currently supported so we can reuse the same definitions for building all items
        caseSearchResult.getCases().forEach(caseDetails ->
            items.add(buildSearchResultViewItem(caseDetails, caseTypeDefinition, searchResultDefinition)));

        return items;
    }

    private List<SearchResultViewHeaderGroup> buildHeaders(String caseTypeId, String useCase, CaseSearchResult caseSearchResult) {
        List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
        SearchResultViewHeaderGroup caseSearchHeader = buildHeader(useCase, caseSearchResult, caseTypeId, getCaseTypeDefinition(caseTypeId));
        headers.add(caseSearchHeader);

        return headers;
    }

    private CaseTypeDefinition getCaseTypeDefinition(String caseTypeId) {
        return caseTypeService.getCaseType(caseTypeId);
    }

    private SearchResultViewHeaderGroup buildHeader(String useCase, CaseSearchResult caseSearchResult, String caseTypeId, CaseTypeDefinition caseType) {
        SearchResult searchResult = searchQueryOperation.getSearchResultDefinition(caseType, useCase);
        if (searchResult.getFields().length == 0) {
            throw new BadSearchRequest(String.format("The provided use case '%s' is unsupported for case type '%s'.",
                useCase, caseType.getId()));
        }
        return new SearchResultViewHeaderGroup(
            new HeaderGroupMetadata(caseType.getJurisdictionId(), caseTypeId),
            buildSearchResultViewColumns(caseType, searchResult),
            caseSearchResult.getCaseReferences(caseTypeId)
        );
    }

    private List<SearchResultViewHeader> buildSearchResultViewColumns(CaseTypeDefinition caseTypeDefinition,
                                                                      SearchResult searchResult) {
        HashSet<String> addedFields = new HashSet<>();

        return Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseTypeDefinition.getCaseFieldDefinitions().stream()
                .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                .filter(caseField -> filterDistinctFieldsByRole(addedFields, searchResultField))
                .map(caseField -> buildSearchResultViewColumn(searchResultField, caseField))
            )
            .collect(Collectors.toList());
    }

    private SearchResultViewHeader buildSearchResultViewColumn(SearchResultField searchResultField,
                                                               CaseFieldDefinition caseFieldDefinition) {
        CommonField commonField = commonField(searchResultField, caseFieldDefinition);
        return new SearchResultViewHeader(
            searchResultField.buildCaseFieldId(),
            commonField.getFieldTypeDefinition(),
            searchResultField.getLabel(),
            searchResultField.getDisplayOrder(),
            searchResultField.isMetadata(),
            displayContextParameter(searchResultField, commonField));
    }

    private boolean filterDistinctFieldsByRole(HashSet<String> addedFields, SearchResultField resultField) {
        String id = resultField.buildCaseFieldId();
        if (addedFields.contains(id)) {
            return false;
        } else {
            if (StringUtils.isEmpty(resultField.getRole()) || userRepository.getUserRoles().contains(resultField.getRole())) {
                addedFields.add(id);
                return true;
            } else {
                return false;
            }
        }
    }

    private CommonField commonField(SearchResultField searchResultField, CaseFieldDefinition caseFieldDefinition) {
        return caseFieldDefinition.getComplexFieldNestedField(searchResultField.getCaseFieldPath())
            .orElseThrow(() ->
                new BadRequestException(format("CaseField %s has no nested elements with code %s.",
                    caseFieldDefinition.getId(), searchResultField.getCaseFieldPath())));
    }

    private String displayContextParameter(SearchResultField searchResultField, CommonField commonField) {
        return searchResultField.getDisplayContextParameter() == null
            ? commonField.getDisplayContextParameter()
            : searchResultField.getDisplayContextParameter();
    }

    private SearchResultViewItem buildSearchResultViewItem(CaseDetails caseDetails,
                                                           CaseTypeDefinition caseTypeDefinition,
                                                           SearchResult searchResult) {
        Map<String, Object> caseFields = prepareData(
            searchResult,
            caseDetails.getData(),
            caseDetails.getMetadata(),
            caseTypeDefinition.getLabelsFromCaseFields()
        );

        return new SearchResultViewItem(caseDetails.getReferenceAsString(), caseFields, new HashMap<>(caseFields));
    }

    private Map<String, Object> prepareData(SearchResult searchResult,
                                            Map<String, JsonNode> caseData,
                                            Map<String, Object> metadata,
                                            Map<String, TextNode> labels) {
        Map<String, Object> newResults = new HashMap<>();

        searchResult.getFieldsWithPaths().forEach(searchResultField -> {
            JsonNode topLevelCaseFieldNode = caseData.get(searchResultField.getCaseFieldId());
            if (topLevelCaseFieldNode != null) {
                newResults.put(searchResultField.buildCaseFieldId(),
                    getNestedCaseFieldByPath(topLevelCaseFieldNode, searchResultField.getCaseFieldPath()));
            }
        });

        newResults.putAll(caseData);
        newResults.putAll(labels);
        newResults.putAll(metadata);
        return newResults;
    }
}
