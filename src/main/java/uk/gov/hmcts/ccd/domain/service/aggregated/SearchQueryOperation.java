package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DraftAccessException;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.service.getdraft.DefaultGetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.processor.SearchInputProcessor;
import uk.gov.hmcts.ccd.domain.service.search.AuthorisedSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.SearchOperation;

import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
public class SearchQueryOperation {
    protected static final String NO_ERROR = null;
    public static final String WORKBASKET = "WORKBASKET";

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

        Optional<CaseType> caseType = this.getCaseTypeOperation.execute(metadata.getCaseTypeId(), CAN_READ);

        if (!caseType.isPresent()) {
            return new SearchResultView(Collections.emptyList(), Collections.emptyList(), NO_ERROR);
        }

        final SearchResult searchResult = getSearchResultDefinition(caseType.get(), view);

        addSortOrderFields(metadata, searchResult);

        final Map<String, String> criteria = searchInputProcessor.execute(view, metadata, queryParameters);
        final List<CaseDetails> cases = searchOperation.execute(metadata, criteria);

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

    private SearchResult getSearchResultDefinition(final CaseType caseType, final String view) {
        if (WORKBASKET.equalsIgnoreCase(view)) {
            return uiDefinitionRepository.getWorkBasketResult(caseType.getId());
        }
        return uiDefinitionRepository.getSearchResult(caseType.getId());
    }

    private void addSortOrderFields(MetaData metadata,SearchResult searchResult) {
        List<SortOrderField> sortOrders = getSortOrders(searchResult);
        metadata.setSortOrderFields(sortOrders);
    }

    private List<SortOrderField> getSortOrders(SearchResult searchResult) {
        return Arrays.stream(searchResult.getFields())
            .filter(searchResultField -> hasSortField(searchResultField))
            .filter(searchResultField -> filterByRole(searchResultField))
            .sorted(Comparator.comparing(srf -> srf.getSortOrder().getPriority()))
            .map(this::toSortOrderField)
            .collect(Collectors.toList());
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
