package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.globalsearch.OtherCaseReference;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchParty;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.SEARCH_CRITERIA;

@Slf4j
@Service
public class GlobalSearchProcessorService {

    public Map<String, JsonNode> populateGlobalSearchData(CaseTypeDefinition caseTypeDefinition,
                                                          Map<String, JsonNode> data) {

        // NB: only need to generate GlobalSearch data if 'SearchCriteria' field is present in the case type definition
        if (caseTypeDefinition.getCaseField(SEARCH_CRITERIA).isPresent()) {
            Map<String, JsonNode> clonedData = null;
            if (data != null) {
                clonedData = new HashMap<>(data);
            }

            List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias =
                caseTypeDefinition.getSearchCriterias();

            List<uk.gov.hmcts.ccd.domain.model.definition.SearchParty> searchParties =
                caseTypeDefinition.getSearchParties();

            SearchCriteria searchCriteria = populateSearchCriteria(clonedData, searchCriterias);
            List<SearchParty> searchPartyList = populateSearchParties(clonedData, searchParties);

            if (!searchPartyList.isEmpty()) {
                searchCriteria.setSearchParties(searchPartyList);
            }

            // NB: always output data with 'SearchCriteria' field even if empty; as used by logstash process as a
            //     trigger to tell it to send a subset of the case data to the Global Search index
            if (clonedData == null) {
                clonedData = new HashMap<>();
            }
            clonedData.put(SEARCH_CRITERIA, JacksonUtils.convertValueJsonNode(searchCriteria));

            return clonedData;
        } else {
            return data;
        }
    }

    private SearchCriteria populateSearchCriteria(Map<String, JsonNode> data,
                                                  List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria>
                                                      searchCriterias) {
        List<OtherCaseReference> otherCaseReferences = new ArrayList<>();
        SearchCriteria returnValue = new SearchCriteria();

        if (data != null) {
            searchCriterias.forEach(currentSearchCriteria -> data.forEach((key, jsonNode) -> {
                if (currentSearchCriteria.getOtherCaseReference().contains(".")
                    && key.equals(currentSearchCriteria.getOtherCaseReference().split("\\.")[0])) {
                    otherCaseReferences.add(OtherCaseReference.builder()
                        .id(UUID.randomUUID().toString())
                        .value(getNestedValue(jsonNode, currentSearchCriteria.getOtherCaseReference()))
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
                                                    List<uk.gov.hmcts.ccd.domain.model.definition.SearchParty>
                                                        searchParties) {
        List<SearchParty> searchPartyList = new ArrayList<>();

        searchParties.forEach(searchParty -> {
            SearchPartyValue valueToPopulate = populateSearchParty(searchParty, data);

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

    private static boolean isComplexField(String field, String key) {
        return field.contains(".") && key.equals(field.split("\\.")[0]);
    }

    private String getNestedValue(JsonNode jsonNode, String field) {
        JsonNode nestedCaseFieldByPath =
            CaseFieldPathUtils.getNestedCaseFieldByPath(jsonNode, field.substring(field.indexOf(".") + 1));
        return nestedCaseFieldByPath != null ? nestedCaseFieldByPath.textValue() : null;
    }

    private SearchPartyValue populateSearchParty(
        uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchPartyDefinition,
        Map<String, JsonNode> data) {

        Map<String, String> namesToValuesMap = new LinkedHashMap<>();
        SearchPartyValue searchPartyValue = new SearchPartyValue();

        if (searchPartyDefinition.getSearchPartyName() != null) {

            List<String> searchPartyNames = Arrays.asList(searchPartyDefinition.getSearchPartyName().split(","));
            searchPartyNames.forEach(searchPartyName -> namesToValuesMap.putIfAbsent(searchPartyName, ""));

            searchPartyNames.forEach(currentSearchPartyName -> {
                String searchPartyName = findValueInMap(currentSearchPartyName, data);

                if (searchPartyName != null) {
                    namesToValuesMap.replace(currentSearchPartyName, searchPartyName);
                }
            });

            String name = namesToValuesMap.keySet()
                .stream()
                .map(namesToValuesMap::get)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));

            if (!name.isBlank()) {
                searchPartyValue.setName(name);
            }
        }

        searchPartyValue.setAddressLine1(findValueInMap(searchPartyDefinition.getSearchPartyAddressLine1(), data));
        searchPartyValue.setEmailAddress(findValueInMap(searchPartyDefinition.getSearchPartyEmailAddress(), data));
        searchPartyValue.setPostCode(findValueInMap(searchPartyDefinition.getSearchPartyPostCode(), data));
        searchPartyValue.setDateOfBirth(
            findDateValueInMap(searchPartyDefinition.getSearchPartyDob(), data, searchPartyDefinition.getCaseTypeId()));
        searchPartyValue.setDateOfDeath(
            findDateValueInMap(searchPartyDefinition.getSearchPartyDod(), data, searchPartyDefinition.getCaseTypeId()));

        return searchPartyValue;
    }

    private String findValueInMap(String valueToFind, Map<String, JsonNode> mapToSearch) {
        String returnValue = null;

        if (valueToFind != null && mapToSearch != null) {
            for (Map.Entry<String, JsonNode> entry : mapToSearch.entrySet()) {
                boolean processedValue = false;

                if (isComplexField(valueToFind, entry.getKey())) {
                    returnValue = getNestedValue(entry.getValue(), valueToFind);
                    processedValue = true; // i.e. processed as a nested value

                } else if (entry.getKey().equals(valueToFind)) {
                    returnValue = entry.getValue().textValue();
                    processedValue = true;
                }

                if (processedValue) {
                    break; // NB: only one break per loop as per S135
                }
            }
        }
        return returnValue;
    }

    private String findDateValueInMap(String valueToFind, Map<String, JsonNode> mapToSearch, String caseTypeID) {
        String value = findValueInMap(valueToFind, mapToSearch);
        if (value != null) {
            try {
                LocalDate.parse(value, ISO_DATE);
            } catch (DateTimeParseException e) {
                log.warn("The value: {} for CaseTypeID: {} is not a Date value in the ISO format", value, caseTypeID);
                return null;
            }
        }
        return value;
    }
}
