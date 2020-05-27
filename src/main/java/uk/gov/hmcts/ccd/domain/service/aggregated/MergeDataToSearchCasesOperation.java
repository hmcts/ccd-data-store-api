package uk.gov.hmcts.ccd.domain.service.aggregated;

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
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
public class MergeDataToSearchCasesOperation {

    private final UserRepository userRepository;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final SearchQueryOperation searchQueryOperation;

    public MergeDataToSearchCasesOperation(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                           @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation,
                                           final SearchQueryOperation searchQueryOperation) {
        this.userRepository = userRepository;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.searchQueryOperation = searchQueryOperation;
    }

    public UICaseSearchResult execute(final List<String> caseTypeIds,
                                      final CaseSearchResult caseSearchResult,
                                      final UseCase useCase) {
        return new UICaseSearchResult(
            buildHeaders(caseTypeIds, useCase, caseSearchResult),
            buildItems(useCase, caseSearchResult),
            caseSearchResult.getTotal()
        );
    }

    private List<SearchResultViewItem> buildItems(UseCase useCase, CaseSearchResult caseSearchResult) {
        List<SearchResultViewItem> items = new ArrayList<>();
        caseSearchResult.getCases().forEach(caseDetails -> {
            getCaseTypeDefinition(caseDetails.getCaseTypeId()).ifPresent(caseType -> {
                final SearchResult searchResult = searchQueryOperation.getSearchResultDefinition(caseType, useCase);
                items.add(buildSearchResultViewItem(caseDetails, caseType, searchResult));
            });
        });

        return items;
    }

    private List<UICaseSearchHeader> buildHeaders(List<String> caseTypeIds, UseCase useCase, CaseSearchResult caseSearchResult) {
        List<UICaseSearchHeader> headers = new ArrayList<>();
        caseTypeIds.forEach(caseTypeId -> {
            getCaseTypeDefinition(caseTypeId).ifPresent(caseType -> {
                UICaseSearchHeader caseSearchHeader = buildHeader(useCase, caseSearchResult, caseTypeId, caseType);
                headers.add(caseSearchHeader);
            });
        });

        return headers;
    }

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(String caseTypeId) {
        return getCaseTypeOperation.execute(caseTypeId, CAN_READ);
    }

    private UICaseSearchHeader buildHeader(UseCase useCase, CaseSearchResult caseSearchResult, String caseTypeId, CaseTypeDefinition caseType) {
        final SearchResult searchResult = searchQueryOperation.getSearchResultDefinition(caseType, useCase);
        return new UICaseSearchHeader(
            new UICaseSearchHeaderMetadata(caseType.getJurisdictionId(), caseTypeId),
            buildSearchResultViewColumns(caseType, searchResult),
            caseSearchResult.buildCaseReferenceList(caseTypeId)
        );
    }

    private List<SearchResultViewColumn> buildSearchResultViewColumns(final CaseTypeDefinition caseTypeDefinition,
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

    private SearchResultViewColumn buildSearchResultViewColumn(final SearchResultField searchResultField,
                                                               final CaseFieldDefinition caseFieldDefinition) {
        CommonField commonField = commonField(searchResultField, caseFieldDefinition);
        return new SearchResultViewColumn(
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
                    searchResultField.getObjectByPath(jsonNode));
            }
        });

        newResults.putAll(caseData);
        newResults.putAll(labels);
        newResults.putAll(metadata);
        return newResults;
    }
}
