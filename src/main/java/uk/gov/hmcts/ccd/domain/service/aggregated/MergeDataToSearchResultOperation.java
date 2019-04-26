package uk.gov.hmcts.ccd.domain.service.aggregated;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.LABEL;

import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

@Named
@Singleton
public class MergeDataToSearchResultOperation {
    protected static final String WORKBASKET_VIEW = "WORKBASKET";
    private final UIDefinitionRepository uiDefinitionRepository;
    private final UserRepository userRepository;

    public MergeDataToSearchResultOperation(final UIDefinitionRepository uiDefinitionRepository,
                                            @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.userRepository = userRepository;
    }

    public SearchResultView execute(final CaseType caseType,
                                    final List<CaseDetails> caseDetails,
                                    final String view,
                                    final String resultError) {
        final SearchResult searchResult = getSearchResult(caseType, view);
        final HashSet<String> addedFields = new HashSet<>();
        final List<SearchResultViewColumn> viewColumns = Arrays.stream(searchResult.getFields())
            .flatMap(searchResultField -> caseType.getCaseFields()
                .stream()
                .filter(caseField -> caseField.getId().equals(searchResultField.getCaseFieldId()))
                .filter(caseField -> filterDistinctFieldsByRole(addedFields, searchResultField))
                .map(caseField -> createSearchResultViewColumn(searchResultField, caseField))
            )
            .collect(Collectors.toList());

        final List<SearchResultViewItem> viewItems = caseDetails.stream()
            .map(caseData -> buildSearchResultViewItem(caseData, caseType))
            .collect(Collectors.toList());
        return new SearchResultView(viewColumns, viewItems, resultError);
    }

    private SearchResultViewColumn createSearchResultViewColumn(final SearchResultField searchResultField, final CaseField caseField) {
        return new SearchResultViewColumn(
            searchResultField.getCaseFieldId(),
            caseField.getFieldType(),
            searchResultField.getLabel(),
            searchResultField.getDisplayOrder(),
            searchResultField.isMetadata());
    }

    private boolean filterDistinctFieldsByRole(final HashSet<String> addedFields, final SearchResultField resultField) {
        if (!addedFields.contains(resultField.getCaseFieldId())) {
            if (StringUtils.isEmpty(resultField.getRole()) || userRepository.getUserRoles().contains(resultField.getRole())) {
               addedFields.add(resultField.getCaseFieldId());
               return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    private SearchResultViewItem buildSearchResultViewItem(final CaseDetails caseData, final CaseType caseType) {
        return new SearchResultViewItem(caseData.hasCaseReference() ? caseData.getReferenceAsString() : caseData.getId(),
                                        getCaseDataAndMetadata(caseData, caseType));
    }

    private Map<String, Object> getCaseDataAndMetadata(CaseDetails caseDetails, CaseType caseType) {
        Map map = new HashMap<>(caseDetails.getCaseDataAndMetadata());
        caseType.getCaseFields()
            .stream()
            .filter(caseField -> LABEL.equals(caseField.getFieldType().getType()))
            .forEach(f -> map.put(f.getId(), instance.textNode(f.getLabel())));
        return map;
    }

    private SearchResult getSearchResult(final CaseType caseType, final String view) {
        if (WORKBASKET_VIEW.equalsIgnoreCase(view)) {
            return uiDefinitionRepository.getWorkBasketResult(caseType.getId());
        } else {
            return uiDefinitionRepository.getSearchResult(caseType.getId());
        }
    }
}
