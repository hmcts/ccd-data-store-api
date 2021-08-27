package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.globalsearch.OtherCaseReference;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchParty;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GlobalSearchProcessorService {

    public Map<String, JsonNode> populateGlobalSearchData(CaseTypeDefinition caseTypeDefinition,
                                                          Map<String, JsonNode> data) {
        List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias =
            caseTypeDefinition.getSearchCriterias();

        List<uk.gov.hmcts.ccd.domain.model.definition.SearchParty> searchParties =
            caseTypeDefinition.getSearchParties();

        SearchCriteria searchCriteria = populateSearchCriteria(data, searchCriterias);
        List<SearchParty> searchPartyList = populateSearchParties(data, searchParties);

        if (!searchPartyList.isEmpty()) {
            searchCriteria.setSearchParties(searchPartyList);
        }

        if (searchCriteria != null && !searchCriteria.isEmpty()) {
            data.put("SearchCriteria", JacksonUtils.convertValueJsonNode(searchCriteria));
        }

        return data;
    }

    private SearchCriteria populateSearchCriteria(Map<String, JsonNode> data,
                                       List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias) {
        List<OtherCaseReference> otherCaseReferences = new ArrayList<>();
        SearchCriteria returnValue = null;

        if (data != null) {
            data.forEach((key, jsonNode) -> {
                searchCriterias.forEach(currentSearchCriteria -> {

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
                });
            });

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
            String searchPartyName = searchPartyDefinition.getSearchPartyName();

            if (isComplexField(searchPartyName, key)) {
                searchPartyValue.setName(getNestedValue(jsonNode, searchPartyName));
            } else if (key.equals(searchPartyName)) {
                searchPartyValue.setName(jsonNode.textValue());
            }
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
