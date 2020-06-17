package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.SearchInputProcessor;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getPathElements;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getPathElementsTailAsString;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Slf4j
public class SearchQueryOperation {
    protected static final String NO_ERROR = null;
    public static final String WORKBASKET = "WORKBASKET";
    public static final String SEARCH = "SEARCH";

    private final MergeDataToSearchResultOperation mergeDataToSearchResultOperation;
    private final GetCaseTypeOperation getCaseTypeOperation;
    private final SearchOperation searchOperation;
    private final GetDraftsOperation getDraftsOperation;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final UserRepository userRepository;
    private final SearchInputProcessor searchInputProcessor;

    @Autowired
    public SearchQueryOperation(@Qualifier(AuthorisedSearchOperation.QUALIFIER) final SearchOperation searchOperation,
                                final MergeDataToSearchResultOperation mergeDataToSearchResultOperation,
                                @Qualifier(AuthorisedGetCaseTypeOperation.QUALIFIER) final GetCaseTypeOperation getCaseTypeOperation,
                                @Qualifier(DefaultGetDraftsOperation.QUALIFIER) GetDraftsOperation getDraftsOperation,
                                final UIDefinitionRepository uiDefinitionRepository,
                                @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                final SearchInputProcessor searchInputProcessor) {
        this.searchOperation = searchOperation;
        this.mergeDataToSearchResultOperation = mergeDataToSearchResultOperation;
        this.getCaseTypeOperation = getCaseTypeOperation;
        this.getDraftsOperation = getDraftsOperation;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.userRepository = userRepository;
        this.searchInputProcessor = searchInputProcessor;
    }

    public SearchResultView execute(final String view,
                                    final MetaData metadata,
                                    final Map<String, String> queryParameters) {

        Optional<CaseTypeDefinition> caseType = this.getCaseTypeOperation.execute(metadata.getCaseTypeId(), CAN_READ);

        if (!caseType.isPresent()) {
            return new SearchResultView(Collections.emptyList(), Collections.emptyList(), NO_ERROR);
        }

        final SearchResult searchResult = getSearchResultDefinition(caseType.get(),
            Strings.isNullOrEmpty(view) ? SEARCH : view, Collections.emptyList());

        addSortOrderFields(metadata, searchResult);

        final List<CaseDetails> cases = searchOperation.execute(
            searchInputProcessor.executeMetadata(view, metadata),
            searchInputProcessor.executeQueryParams(view, metadata, queryParameters)
        );

        String draftResultError = NO_ERROR;
        List<CaseDetails> draftsAndCases = Lists.newArrayList();
        if (StringUtils.equalsAnyIgnoreCase(WORKBASKET, view) && caseType.get().hasDraftEnabledEvent()) {
            try {
                draftsAndCases = getDraftsOperation.execute(metadata);
            } catch (DraftAccessException dae) {
                draftResultError = dae.getMessage();
            }
        }
        draftsAndCases.addAll(cases);
        return mergeDataToSearchResultOperation.execute(caseType.get(), searchResult, draftsAndCases, draftResultError);
    }

    public SearchResult getSearchResultDefinition(CaseTypeDefinition caseTypeDefinition, String useCase, List<String> requestedFields) {
        String caseTypeId = caseTypeDefinition.getId();
        if (Strings.isNullOrEmpty(useCase)) {
            return buildSearchResultDefinitionFromCaseFields(caseTypeDefinition, requestedFields);
        }
        // TODO: Once all *ResultFields tabs are merged, remove switch statement and always call default method
        switch (useCase) {
            case WORKBASKET:
                return uiDefinitionRepository.getWorkBasketResult(caseTypeId);
            case SEARCH:
                return uiDefinitionRepository.getSearchResult(caseTypeId);
            default:
                return uiDefinitionRepository.getSearchCasesResult(caseTypeId, useCase);
        }
    }

    public List<SortOrderField> getSortOrders(CaseTypeDefinition caseType, String useCase) {
        return getSortOrders(getSearchResultDefinition(caseType, useCase, Collections.emptyList()));
    }

    private List<SortOrderField> getSortOrders(SearchResult searchResult) {
        return Arrays.stream(searchResult.getFields())
            .filter(this::hasSortField)
            .filter(this::filterByRole)
            .sorted(Comparator.comparing(srf -> srf.getSortOrder().getPriority()))
            .map(this::toSortOrderField)
            .collect(Collectors.toList());
    }

    private SearchResultField buildSearchResultField(CaseFieldDefinition caseFieldDefinition, String caseFieldId, String caseFieldPath) {
        SearchResultField searchResultField = new SearchResultField();
        searchResultField.setCaseFieldId(caseFieldDefinition.getId());
        searchResultField.setCaseTypeId(caseFieldDefinition.getCaseTypeId());
        searchResultField.setLabel(caseFieldDefinition.getLabel());
        searchResultField.setMetadata(caseFieldDefinition.isMetadata());
        searchResultField.setCaseFieldId(caseFieldId);
        searchResultField.setCaseFieldPath(caseFieldPath);
        return searchResultField;
    }

    private SearchResult buildSearchResultDefinitionFromCaseFields(CaseTypeDefinition caseTypeDefinition, List<String> requestedFields) {
        SearchResult searchResult = new SearchResult();
        List<SearchResultField> searchResultFields = new ArrayList<>();

        if (CollectionUtils.isEmpty(requestedFields)) {
            caseTypeDefinition.getCaseFieldDefinitions().forEach(field ->
                searchResultFields.add(buildSearchResultField(field, field.getId(), null)));
        } else {
            requestedFields.forEach(requestedFieldId -> {
                caseTypeDefinition.getComplexSubfieldDefinitionByPath(requestedFieldId).ifPresent(field -> {
                    List<String> pathElements = getPathElements(requestedFieldId);
                    searchResultFields.add(buildSearchResultField((CaseFieldDefinition) field, pathElements.get(0),
                        pathElements.size() > 1 ? getPathElementsTailAsString(pathElements) : null));
                });
            });
        }

        searchResult.setFields(searchResultFields.toArray(new SearchResultField[0]));
        return searchResult;
    }

    private void addSortOrderFields(MetaData metadata, SearchResult searchResult) {
        List<SortOrderField> sortOrders = getSortOrders(searchResult);
        metadata.setSortOrderFields(sortOrders);
    }

    private boolean hasSortField(SearchResultField searchResultField) {
        SortOrder sortOrder = searchResultField.getSortOrder();
        return sortOrder != null && sortOrder.getDirection() != null && sortOrder.getPriority() != null;
    }

    private boolean filterByRole(SearchResultField resultField) {
        return StringUtils.isEmpty(resultField.getRole()) || userRepository.getUserRoles().contains(resultField.getRole());
    }

    private SortOrderField toSortOrderField(SearchResultField searchResultField) {
        return SortOrderField.sortOrderWith()
            .caseFieldId(searchResultField.buildCaseFieldId())
            .metadata(searchResultField.isMetadata())
            .direction(searchResultField.getSortOrder().getDirection())
            .build();
    }

}
