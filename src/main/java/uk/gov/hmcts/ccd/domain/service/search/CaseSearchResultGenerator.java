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
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.processor.SearchResultProcessor;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
public class CaseSearchResultGenerator {

    private final UserRepository userRepository;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final SearchQueryOperation searchQueryOperation;
    private final SearchResultProcessor searchResultProcessor;

    public CaseSearchResultGenerator(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                     @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) GetCaseTypeOperation getCaseTypeOperation,
                                     SearchQueryOperation searchQueryOperation,
                                     SearchResultProcessor searchResultProcessor) {
        this.userRepository = userRepository;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.searchQueryOperation = searchQueryOperation;
        this.searchResultProcessor = searchResultProcessor;
    }

    public UICaseSearchResult execute(String caseTypeId,
                                      CaseSearchResult caseSearchResult,
                                      String useCase) {
        List<SearchResultViewHeaderGroup> headerGroups = buildHeaders(caseTypeId, useCase, caseSearchResult);
        List<SearchResultViewItem> items = buildItems(useCase, caseSearchResult);

        if (itemsRequireFormatting(headerGroups)) {
            items = searchResultProcessor.execute(headerGroups.get(0).getFields(), items);
        }

        // TODO: Filter out fields from result that haven't been requested before returning (RDM-8556)
        return new UICaseSearchResult(
            headerGroups,
            items,
            caseSearchResult.getTotal(),
            useCase
        );
    }

    private boolean itemsRequireFormatting(List<SearchResultViewHeaderGroup> headerGroups) {
        return headerGroups.size() == 1
               && headerGroups.get(0).getFields().stream().anyMatch(field -> field.getDisplayContextParameter() != null);
    }

    private List<SearchResultViewItem> buildItems(String useCase, CaseSearchResult caseSearchResult) {
        List<SearchResultViewItem> items = new ArrayList<>();
        caseSearchResult.getCases().forEach(caseDetails -> {
            getCaseTypeDefinition(caseDetails.getCaseTypeId()).ifPresent(caseType -> {
                final SearchResult searchResult = searchQueryOperation.getSearchResultDefinition(caseType, useCase);
                items.add(buildSearchResultViewItem(caseDetails, caseType, searchResult));
            });
        });

        return items;
    }

    private List<SearchResultViewHeaderGroup> buildHeaders(String caseTypeId, String useCase, CaseSearchResult caseSearchResult) {
        List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
        getCaseTypeDefinition(caseTypeId).ifPresent(caseType -> {
            SearchResultViewHeaderGroup caseSearchHeader = buildHeader(useCase, caseSearchResult, caseTypeId, caseType);
            headers.add(caseSearchHeader);
        });

        return headers;
    }

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(String caseTypeId) {
        return getCaseTypeOperation.execute(caseTypeId, CAN_READ);
    }

    private SearchResultViewHeaderGroup buildHeader(String useCase, CaseSearchResult caseSearchResult, String caseTypeId, CaseTypeDefinition caseType) {
        final SearchResult searchResult = searchQueryOperation.getSearchResultDefinition(caseType, useCase);
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

    private List<SearchResultViewHeader> buildSearchResultViewColumns(final CaseTypeDefinition caseTypeDefinition,
                                                                      final SearchResult searchResult) {
        final HashSet<String> addedFields = new HashSet<>();

        return Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseTypeDefinition.getCaseFieldDefinitions().stream()
                .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                .filter(caseField -> filterDistinctFieldsByRole(addedFields, searchResultField))
                .map(caseField -> buildSearchResultViewColumn(searchResultField, caseField))
            )
            .collect(Collectors.toList());
    }

    private SearchResultViewHeader buildSearchResultViewColumn(final SearchResultField searchResultField,
                                                               final CaseFieldDefinition caseFieldDefinition) {
        CommonField commonField = commonField(searchResultField, caseFieldDefinition);
        return new SearchResultViewHeader(
            searchResultField.buildCaseFieldId(),
            commonField.getFieldTypeDefinition(),
            searchResultField.getLabel(),
            searchResultField.getDisplayOrder(),
            searchResultField.isMetadata(),
            displayContextParameter(searchResultField, commonField));
    }

    private boolean filterDistinctFieldsByRole(final HashSet<String> addedFields, final SearchResultField resultField) {
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

    private SearchResultViewItem buildSearchResultViewItem(final CaseDetails caseDetails,
                                                           final CaseTypeDefinition caseTypeDefinition,
                                                           final SearchResult searchResult) {

        Map<String, JsonNode> caseData = new HashMap<>(caseDetails.getData());
        Map<String, Object> caseMetadata = new HashMap<>(caseDetails.getMetadata());
        Map<String, TextNode> labels = caseTypeDefinition.getLabelsFromCaseFields();
        Map<String, Object> caseFields = prepareData(searchResult, caseData, caseMetadata, labels);

        String caseId = caseDetails.hasCaseReference() ? caseDetails.getReferenceAsString() : caseDetails.getId();
        return new SearchResultViewItem(caseId, caseFields, new HashMap<>(caseFields));
    }

    private Map<String, Object> prepareData(SearchResult searchResult,
                                            Map<String, JsonNode> caseData,
                                            Map<String, Object> metadata,
                                            Map<String, TextNode> labels) {

        Map<String, Object> newResults = new HashMap<>();

        searchResult.getFieldsWithPaths().forEach(searchResultField -> {
            JsonNode jsonNode = caseData.get(searchResultField.getCaseFieldId());
            if (jsonNode != null) {
                newResults.put(searchResultField.getCaseFieldId() + "." + searchResultField.getCaseFieldPath(),
                    searchResultField.getCaseFieldNode(jsonNode));
            }
        });

        newResults.putAll(caseData);
        newResults.putAll(labels);
        newResults.putAll(metadata);
        return newResults;
    }
}
