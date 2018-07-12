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
import java.util.stream.Collectors;

@Named
@Singleton
public class MergeDataToSearchResultOperation {
    private final UIDefinitionRepository uiDefinitionRepository;

    public MergeDataToSearchResultOperation(final UIDefinitionRepository uiDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
    }

    public SearchResultView execute(final CaseType caseType, final List<CaseDetails> caseDetails,final String view) {
        final SearchResult searchResult = getSearchResult(caseType, view);
        final List<SearchResultViewColumn> viewColumns = Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseType.getCaseFields().stream()
            .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
            .map(caseField ->  new SearchResultViewColumn(
                searchResultField.getCaseFieldId(),
                caseField.getFieldType(),
                searchResultField.getLabel(),
                searchResultField.getDisplayOrder())
             ))
            .collect(Collectors.toList());

        final List<SearchResultViewItem> viewItems = caseDetails.stream()
            .map(caseData -> new SearchResultViewItem(caseData.getReference().toString())
                .addCaseFields(caseData.getData())
                .addCaseFields(caseData.getMetadata()))
            .collect(Collectors.toList());
        return new SearchResultView(viewColumns, viewItems);
    }

    private SearchResult getSearchResult(final CaseType caseType, final String view) {
        final SearchResult searchResult;
        if (StringUtils.equalsAnyIgnoreCase("WORKBASKET", view))        {
            searchResult= uiDefinitionRepository.getWorkBasketResult(caseType.getId());
        } else {
            searchResult= uiDefinitionRepository.getSearchResult(caseType.getId());
        }
        return searchResult;
    }
}
