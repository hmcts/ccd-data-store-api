package uk.gov.hmcts.ccd.domain.service.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.*;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.processor.SearchResultProcessor;
import uk.gov.hmcts.ccd.endpoint.exceptions.*;

import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getNestedCaseFieldByPath;

@Service
public class CaseSearchResultViewGenerator {

    private final UserRepository userRepository;
    private final CaseTypeService caseTypeService;
    private final SearchResultDefinitionService searchResultDefinitionService;
    private final SearchResultProcessor searchResultProcessor;
    private final CaseSearchesViewAccessControl caseSearchesViewAccessControl;

    public CaseSearchResultViewGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                         CaseTypeService caseTypeService,
                                         SearchResultDefinitionService searchResultDefinitionService,
                                         SearchResultProcessor searchResultProcessor,
                                         CaseSearchesViewAccessControl caseSearchesViewAccessControl) {
        this.userRepository = userRepository;
        this.caseTypeService = caseTypeService;
        this.searchResultDefinitionService = searchResultDefinitionService;
        this.searchResultProcessor = searchResultProcessor;
        this.caseSearchesViewAccessControl = caseSearchesViewAccessControl;
    }

    public CaseSearchResultView execute(String caseTypeId,
                                        CaseSearchResult caseSearchResult,
                                        String useCase,
                                        List<String> requestedFields) {

        List<SearchResultViewHeaderGroup> headerGroups = buildHeaders(caseTypeId, useCase, caseSearchResult, requestedFields);
        List<SearchResultViewItem> items = buildItems(useCase, caseSearchResult, caseTypeId, requestedFields);

        if (itemsRequireFormatting(headerGroups)) {
            items = searchResultProcessor.execute(headerGroups.get(0).getFields(), items);
        }

        return new CaseSearchResultView(
            headerGroups,
            items,
            caseSearchResult.getTotal()
        );
    }

    public CaseDetails filterUnauthorisedFieldsByUseCaseAndUserRole(String useCase, CaseDetails caseDetails, String caseTypeId, List<String> requestedFields) {
        caseDetails.getData().entrySet().removeIf(
            caseField -> !caseSearchesViewAccessControl.filterResultsBySearchResultsDefinition(useCase, caseTypeId, requestedFields, caseField.getKey()));
        return caseDetails;
    }

    private boolean itemsRequireFormatting(List<SearchResultViewHeaderGroup> headerGroups) {
        return headerGroups.size() == 1
            && headerGroups.get(0).getFields().stream().anyMatch(field -> field.getDisplayContextParameter() != null);
    }

    private List<SearchResultViewItem> buildItems(String useCase, CaseSearchResult caseSearchResult, String caseTypeId, List<String> requestedFields) {
        CaseTypeDefinition caseTypeDefinition = getCaseTypeDefinition(caseTypeId);
        SearchResultDefinition searchResultDefinition = searchResultDefinitionService.getSearchResultDefinition(caseTypeDefinition, useCase, requestedFields);

        List<SearchResultViewItem> items = new ArrayList<>();
        caseSearchResult.getCases().forEach(caseDetails -> {
            filterUnauthorisedFieldsByUseCaseAndUserRole(useCase, caseDetails, caseTypeId, requestedFields);
            items.add(buildSearchResultViewItem(caseDetails, caseTypeDefinition, searchResultDefinition));
        });
        return items;
    }

    private List<SearchResultViewHeaderGroup> buildHeaders(String caseTypeId, String useCase, CaseSearchResult
        caseSearchResult, List<String> requestedFields) {
        List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
        SearchResultViewHeaderGroup caseSearchHeader = buildHeader(useCase, caseSearchResult, caseTypeId, getCaseTypeDefinition(caseTypeId), requestedFields);
        headers.add(caseSearchHeader);

        return headers;
    }

    private CaseTypeDefinition getCaseTypeDefinition(String caseTypeId) {
        return caseTypeService.getCaseType(caseTypeId);
    }

    private SearchResultViewHeaderGroup buildHeader(String useCase,
                                                    CaseSearchResult caseSearchResult,
                                                    String caseTypeId,
                                                    CaseTypeDefinition caseType,
                                                    List<String> requestedFields) {
        SearchResultDefinition searchResult = searchResultDefinitionService.getSearchResultDefinition(caseType, useCase, requestedFields);
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
                                                                      SearchResultDefinition searchResult) {
        HashSet<String> addedFields = new HashSet<>();

        // Only one case type is currently supported so we can reuse the same definitions for building all items
        return Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseTypeDefinition.getCaseFieldDefinitions().stream()
                .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                .filter(caseField -> filterDistinctFieldsByRole(addedFields, searchResultField))
                .filter(caseField -> caseSearchesViewAccessControl.filterFieldByAuthorisationAccessOnField(caseField))
                .filter(caseField -> caseSearchesViewAccessControl.filterResultsBySecurityClassification(caseField, caseTypeDefinition))
                .map(caseField -> buildSearchResultViewColumn(searchResultField, caseField))
            )
            .collect(toList());
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
                                                           SearchResultDefinition searchResult) {
        Map<String, Object> caseFields = prepareData(
            searchResult,
            caseDetails.getData(),
            caseDetails.getMetadata(),
            caseTypeDefinition.getLabelsFromCaseFields()
        );

        return new SearchResultViewItem(caseDetails.getReferenceAsString(), caseFields, new HashMap<>(caseFields));
    }

    private Map<String, Object> prepareData(SearchResultDefinition searchResult,
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
