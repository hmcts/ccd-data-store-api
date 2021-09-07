package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.globalsearch.OtherCaseReference;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchParty;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GlobalSearchProcessorService {

    private static final String SEARCH_CRITERIA = "SearchCriteria";

    private Map<String, String> namesToValuesMap = new LinkedHashMap<>();

    public Map<String, JsonNode> populateGlobalSearchData(CaseTypeDefinition caseTypeDefinition,
                                                          Map<String, JsonNode> data) {
        List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias =
            caseTypeDefinition.getSearchCriterias();

        List<uk.gov.hmcts.ccd.domain.model.definition.SearchParty> searchParties =
            caseTypeDefinition.getSearchParties();

        Optional<CaseFieldDefinition> searchCriteriaCaseField = caseTypeDefinition.getCaseField(SEARCH_CRITERIA);

        Map<String, JsonNode> clonedData = null;
        if (data != null) {
            clonedData = new HashMap<>(data);
        }

        if (searchCriteriaCaseField.isPresent()) {

            SearchCriteria searchCriteria = populateSearchCriteria(clonedData, searchCriterias);
            List<SearchParty> searchPartyList = populateSearchParties(clonedData, searchParties);

            if (!searchPartyList.isEmpty()) {
                searchCriteria.setSearchParties(searchPartyList);
            }
            if (!searchCriteria.isEmpty() && clonedData != null) {
                clonedData.put(SEARCH_CRITERIA, JacksonUtils.convertValueJsonNode(searchCriteria));
            }
        }

        return clonedData;
    }

    private SearchCriteria populateSearchCriteria(Map<String, JsonNode> data,
                                       List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias) {
        List<OtherCaseReference> otherCaseReferences = new ArrayList<>();
        SearchCriteria returnValue = new SearchCriteria();

        if (data != null) {
            data.forEach((key, jsonNode) -> searchCriterias.forEach(currentSearchCriteria -> {

                if (currentSearchCriteria.getOtherCaseReference().contains(".")
                    && key.equals(currentSearchCriteria.getOtherCaseReference().split("\\.")[0])) {
                    JsonNode nestedCaseFieldByPath =
                        CaseFieldPathUtils.getNestedCaseFieldByPath(jsonNode,
                            currentSearchCriteria.getOtherCaseReference().substring(
                                currentSearchCriteria.getOtherCaseReference().indexOf(".") + 1));
                    otherCaseReferences.add(OtherCaseReference.builder()
                        .id(UUID.randomUUID().toString())
                        .value(nestedCaseFieldByPath.textValue())
                        .build());
                } else if (key.equals(currentSearchCriteria.getOtherCaseReference())) {
                    otherCaseReferences.add(OtherCaseReference.builder()
                        .id(UUID.randomUUID().toString())
                        .value(jsonNode.textValue())
                        .build());
                }
            }));

            returnValue = SearchCriteria.builder().otherCaseReferences(otherCaseReferences).build();
        }

        return returnValue;
    }

    private List<SearchParty> populateSearchParties(Map<String, JsonNode> data,
                                             List<uk.gov.hmcts.ccd.domain.model.definition.SearchParty> searchParties) {
        List<SearchParty> searchPartyList = new ArrayList<>();

        searchParties.forEach(searchParty -> {
            SearchPartyValue valueToPopulate  = new SearchPartyValue();

            data.forEach((key, jsonNode) -> populateSearchParty(searchParty, valueToPopulate, key, jsonNode));

            String name = namesToValuesMap.keySet()
                .stream()
                .map(key -> namesToValuesMap.get(key))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));

            if (name != null && !name.isBlank()) {
                valueToPopulate.setName(name);
                namesToValuesMap.clear();
            }

            if (!valueToPopulate.isEmpty()) {
                searchPartyList.add(SearchParty
                    .builder()
                    .id(UUID.randomUUID().toString())
                    .value(valueToPopulate)
                    .build());
            }
        });

        return searchPartyList;
    }

    private boolean isComplexField(String field, String key) {
        return field.contains(".") && key.equals(field.split("\\.")[0]);
    }

    private String getNestedValue(JsonNode jsonNode, String field) {
        JsonNode nestedCaseFieldByPath =
            CaseFieldPathUtils.getNestedCaseFieldByPath(jsonNode, field.substring(field.indexOf(".") + 1));
        return nestedCaseFieldByPath != null ? nestedCaseFieldByPath.textValue() : null;
    }

    private void populateSearchParty(uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchPartyDefinition,
                                            SearchPartyValue searchPartyValue,
                                            String key,
                                            JsonNode jsonNode) {

        if (searchPartyDefinition.getSearchPartyName() != null) {

            List<String> searchPartyNames = Arrays.asList(searchPartyDefinition.getSearchPartyName().split(","));
            searchPartyNames.forEach(x -> namesToValuesMap.putIfAbsent(x, ""));

            searchPartyNames.forEach(currentSearchPartyName -> {
                String searchPartyName = null;

                if (isComplexField(currentSearchPartyName, key)) {
                    searchPartyName = getNestedValue(jsonNode, currentSearchPartyName);
                } else if (key.equals(currentSearchPartyName)) {
                    searchPartyName = jsonNode.textValue();
                }

                if (searchPartyName != null) {
                    namesToValuesMap.replace(currentSearchPartyName, searchPartyName);
                }
            });
        }

        if (searchPartyDefinition.getSearchPartyAddressLine1() != null) {
            String searchPartyAddressLine = searchPartyDefinition.getSearchPartyAddressLine1();

            if (isComplexField(searchPartyAddressLine, key)) {
                searchPartyValue.setAddressLine1(getNestedValue(jsonNode, searchPartyAddressLine));
            } else if (key.equals(searchPartyAddressLine)) {
                searchPartyValue.setAddressLine1(jsonNode.textValue());
            }
        }

        if (searchPartyDefinition.getSearchPartyEmailAddress() != null) {
            String searchPartyEMailAddress = searchPartyDefinition.getSearchPartyEmailAddress();

            if (isComplexField(searchPartyEMailAddress, key)) {
                searchPartyValue.setEmailAddress(getNestedValue(jsonNode, searchPartyEMailAddress));
            } else if (key.equals(searchPartyEMailAddress)) {
                searchPartyValue.setEmailAddress(jsonNode.textValue());
            }
        }

        if (searchPartyDefinition.getSearchPartyPostCode() != null) {
            String searchPartyPostCode = searchPartyDefinition.getSearchPartyPostCode();

            if (isComplexField(searchPartyPostCode, key)) {
                searchPartyValue.setPostCode(getNestedValue(jsonNode, searchPartyPostCode));
            } else if (key.equals(searchPartyPostCode)) {
                searchPartyValue.setPostCode(jsonNode.textValue());
            }
        }

        if (searchPartyDefinition.getSearchPartyDob() != null) {
            String searchPartyDob = searchPartyDefinition.getSearchPartyDob();

            if (isComplexField(searchPartyDob, key)) {
                searchPartyValue.setDateOfBirth(getNestedValue(jsonNode, searchPartyDob));
            } else if (key.equals(searchPartyDob)) {
                searchPartyValue.setDateOfBirth(jsonNode.textValue());
            }
        }
    }

}
