package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.CaseDetails.LABEL_FIELD_TYPE;

@Named
@Singleton
public class MergeDataToSearchResultOperation {
    private static final String WORKBASKET_VIEW = "WORKBASKET";
    private final UIDefinitionRepository uiDefinitionRepository;

    public MergeDataToSearchResultOperation(final UIDefinitionRepository uiDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
    }

    public SearchResultView execute(final CaseType caseType, final List<CaseDetails> caseDetails, final String view) {
        final SearchResult searchResult = getSearchResult(caseType, view);
        final SearchResultViewColumn[] viewColumns = Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseType.getCaseFields().stream()
            .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
            .map(caseField ->  new SearchResultViewColumn(
                searchResultField.getCaseFieldId(),
                caseField.getFieldType(),
                searchResultField.getLabel(),
                searchResultField.getDisplayOrder())
             ))
            .toArray(SearchResultViewColumn[]::new);

        final SearchResultViewItem[]
            viewItems =
            caseDetails.stream()
                       .map(caseData -> new SearchResultViewItem(caseData.getReference().toString(),
                                                                 caseData.getData(),
                                                                 caseType.getCaseFields()
                                                                         .stream()
                                                                         .filter(caseField -> LABEL_FIELD_TYPE.equals(
                                                                             caseField.getFieldType().getType()))
                                                                         .collect(Collectors.toList())))
                       .toArray(SearchResultViewItem[]::new);
        return new SearchResultView(viewColumns, viewItems);
    }

    private SearchResult getSearchResult(final CaseType caseType, final String view) {
        if (WORKBASKET_VIEW.equalsIgnoreCase(view)) {
            return uiDefinitionRepository.getWorkBasketResult(caseType.getId());
        }
        return uiDefinitionRepository.getSearchResult(caseType.getId());
    }
}
