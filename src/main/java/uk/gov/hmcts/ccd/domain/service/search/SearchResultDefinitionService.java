package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultDefinition;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getPathElements;
import static uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils.getPathElementsTailAsString;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.SEARCH;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;

@Service
public class SearchResultDefinitionService {

    private final UIDefinitionRepository uiDefinitionRepository;

    @Autowired
    public SearchResultDefinitionService(UIDefinitionRepository uiDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
    }

    public SearchResultDefinition getSearchResultDefinition(CaseTypeDefinition caseTypeDefinition, String useCase, List<String> requestedFields) {
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

    private SearchResultDefinition buildSearchResultDefinitionFromCaseFields(CaseTypeDefinition caseTypeDefinition, List<String> requestedFields) {
        SearchResultDefinition searchResult = new SearchResultDefinition();
        List<SearchResultField> searchResultFields = new ArrayList<>();

        if (CollectionUtils.isEmpty(requestedFields)) {
            caseTypeDefinition.getCaseFieldDefinitions().forEach(field ->
                searchResultFields.add(buildSearchResultField(field, field.getId(), null)));
        } else {
            requestedFields.forEach(requestedFieldId ->
                caseTypeDefinition.getComplexSubfieldDefinitionByPath(requestedFieldId).ifPresent(field -> {
                    List<String> pathElements = getPathElements(requestedFieldId);
                    searchResultFields.add(buildSearchResultField((CaseFieldDefinition) field, pathElements.get(0),
                        pathElements.size() > 1 ? getPathElementsTailAsString(pathElements) : null));
                })
            );
        }

        searchResult.setFields(searchResultFields.toArray(new SearchResultField[0]));
        return searchResult;
    }

    private SearchResultField buildSearchResultField(CaseFieldDefinition caseFieldDefinition, String caseFieldId, String caseFieldPath) {
        SearchResultField searchResultField = new SearchResultField();
        searchResultField.setCaseFieldId(caseFieldId);
        searchResultField.setCaseFieldPath(caseFieldPath);
        searchResultField.setCaseTypeId(caseFieldDefinition.getCaseTypeId());
        searchResultField.setLabel(caseFieldDefinition.getLabel());
        searchResultField.setMetadata(caseFieldDefinition.isMetadata());
        return searchResultField;
    }
}
