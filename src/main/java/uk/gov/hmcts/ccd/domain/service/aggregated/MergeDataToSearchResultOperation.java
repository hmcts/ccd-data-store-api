package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetDraftViewOperation.DRAFT_ID;

@Named
@Singleton
public class MergeDataToSearchResultOperation {
    private final UIDefinitionRepository uiDefinitionRepository;

    public MergeDataToSearchResultOperation(final UIDefinitionRepository uiDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
    }

    public SearchResultView execute(final CaseType caseType, final List<CaseDetails> caseDetails, final String view, final String resultError) {
        final SearchResult searchResult = getSearchResult(caseType, view);
        final SearchResultViewColumn[] viewColumns = Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseType.getCaseFields().stream()
                .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                .map(caseField -> new SearchResultViewColumn(
                    searchResultField.getCaseFieldId(),
                    caseField.getFieldType(),
                    searchResultField.getLabel(),
                    searchResultField.getDisplayOrder())
                ))
            .toArray(SearchResultViewColumn[]::new);

        final SearchResultViewItem[] viewItems = caseDetails.stream()
            .map(caseData -> buildSearchResultViewItem(caseData))
            .toArray(SearchResultViewItem[]::new);
        return new SearchResultView(viewColumns, viewItems, resultError);
    }

    private SearchResultViewItem buildSearchResultViewItem(CaseDetails caseData) {
        return new SearchResultViewItem(hasCaseReference(caseData) ? getCaseReference(caseData) : getDraftReference(caseData),
                                        caseData.getData());
    }

    private String getCaseReference(CaseDetails caseData) {
        return caseData.getReference().toString();
    }

    private String getDraftReference(CaseDetails caseData) {
        return String.format(DRAFT_ID, caseData.getId());
    }

    private boolean hasCaseReference(CaseDetails caseData) {
        return caseData.getReference() != null;
    }

    private SearchResult getSearchResult(final CaseType caseType, final String view) {
        final SearchResult searchResult;
        if (StringUtils.equalsAnyIgnoreCase("WORKBASKET", view)) {
            searchResult = uiDefinitionRepository.getWorkBasketResult(caseType.getId());
        } else {
            searchResult = uiDefinitionRepository.getSearchResult(caseType.getId());
        }
        return searchResult;
    }
}
