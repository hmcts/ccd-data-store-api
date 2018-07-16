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

@Named
@Singleton
public class MergeDataToSearchResultOperation {
    public static final String DRAFT_ID = "DRAFT%s";
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
            .map(caseData -> new SearchResultViewItem(hasReference(caseData) ? caseData.getReference().toString() : String.format(DRAFT_ID, caseData.getId()),
                                                      caseData.getData()))
            .toArray(SearchResultViewItem[]::new);
        return new SearchResultView(viewColumns, viewItems, resultError);
    }

    private boolean hasReference(CaseDetails caseData) {
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
