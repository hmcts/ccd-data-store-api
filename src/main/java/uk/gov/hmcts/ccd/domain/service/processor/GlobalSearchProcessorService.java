package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.globalsearch.OtherCaseReference;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchCriteria;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchParty;
import uk.gov.hmcts.ccd.domain.model.globalsearch.SearchPartyValue;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CollectionSanitiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.SEARCH_CRITERIA;

@Service
@Slf4j
public class GlobalSearchProcessorService {

    private static final String JSON_NODE_VALUE_KEY_NAME = CollectionSanitiser.VALUE;

    private final ObjectMapperService objectMapperService;

    @Autowired
    public GlobalSearchProcessorService(ObjectMapperService objectMapperService) {
        this.objectMapperService = objectMapperService;
    }

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
                                       List<uk.gov.hmcts.ccd.domain.model.definition.SearchCriteria> searchCriterias) {
        List<OtherCaseReference> otherCaseReferences = new ArrayList<>();
        SearchCriteria returnValue = new SearchCriteria();

        if (data != null) {
            searchCriterias.forEach(currentSearchCriteria -> data.forEach((key, jsonNode) -> {
                if (currentSearchCriteria.getOtherCaseReference().contains(".")
                    && key.equals(currentSearchCriteria.getOtherCaseReference().split("\\.")[0])) {
                    otherCaseReferences.add(OtherCaseReference.builder()
                        .id(UUID.randomUUID().toString())
                        .value(getNestedValueAsString(jsonNode, currentSearchCriteria.getOtherCaseReference()))
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
            List<SearchPartyValue> valuesToPopulate = populateSearchPartyValues(searchParty, data);

            valuesToPopulate.forEach(valueToPopulate -> {
                if (!valueToPopulate.isEmpty()) {
                    searchPartyList.add(SearchParty
                        .builder()
                        .id(UUID.randomUUID().toString())
                        .value(valueToPopulate)
                        .build());
                }
            });
        });

        return searchPartyList;
    }

    private static boolean isComplexField(String field, String key) {
        return field.contains(".") && key.equals(field.split("\\.")[0]);
    }

    private JsonNode getNestedValueAsJsonNode(JsonNode jsonNode, String field) {
        return CaseFieldPathUtils.getNestedCaseFieldByPath(jsonNode, field.substring(field.indexOf(".") + 1));
    }

    private String getNestedValueAsString(JsonNode jsonNode, String field) {
        JsonNode nestedValueAsNode = getNestedValueAsJsonNode(jsonNode, field);
        return jsonNodeObjectToText(nestedValueAsNode);
    }

    private List<SearchPartyValue> populateSearchPartyValues(
        uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchPartyDefinition,
        Map<String, JsonNode> data) {

        List<SearchPartyValue> searchPartyValues = new ArrayList<>();

        String spCollectionFieldName = searchPartyDefinition.getSearchPartyCollectionFieldName();
        if (!Strings.isNullOrEmpty(spCollectionFieldName)) {
            searchPartyValues = populateSearchPartyValuesFromCollection(searchPartyDefinition, data);
        } else {
            searchPartyValues.add(populateSearchPartyWithoutCollection(searchPartyDefinition, data));
        }

        return searchPartyValues;
    }

    private List<SearchPartyValue> populateSearchPartyValuesFromCollection(
        uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchPartyDefinition,
                                     Map<String, JsonNode> data) {

        List<JsonNode> collectionValueInMap = findCollectionValueInMap(
            searchPartyDefinition, data);

        List<SearchPartyValue> searchPartyValues = new ArrayList<>();

        for (JsonNode node : collectionValueInMap) {
            Map<String, JsonNode> rawValue = getCollectionItemValueFromJsonNode(node);

            SearchPartyValue searchPartyValue = populateSearchPartyWithoutCollection(searchPartyDefinition, rawValue);
            searchPartyValues.add(searchPartyValue);
        }

        return searchPartyValues;
    }

    private SearchPartyValue populateSearchPartyWithoutCollection(
        uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchPartyDefinition,
        Map<String, JsonNode> data) {

        Map<String, String> namesToValuesMap = new LinkedHashMap<>();
        SearchPartyValue searchPartyValue = new SearchPartyValue();

        if (searchPartyDefinition.getSearchPartyName() != null) {

            List<String> searchPartyNames = Arrays.asList(searchPartyDefinition.getSearchPartyName().split(","));
            searchPartyNames.forEach(searchPartyName -> namesToValuesMap.putIfAbsent(searchPartyName, ""));

            searchPartyNames.forEach(currentSearchPartyName -> {
                String searchPartyName = findTextValueInMap(currentSearchPartyName, data);

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

        searchPartyValue.setAddressLine1(findTextValueInMap(searchPartyDefinition.getSearchPartyAddressLine1(), data));
        searchPartyValue.setEmailAddress(findTextValueInMap(searchPartyDefinition.getSearchPartyEmailAddress(), data));
        searchPartyValue.setPostCode(findTextValueInMap(searchPartyDefinition.getSearchPartyPostCode(), data));
        searchPartyValue.setDateOfBirth(findTextValueInMap(searchPartyDefinition.getSearchPartyDob(), data));
        searchPartyValue.setDateOfDeath(findTextValueInMap(searchPartyDefinition.getSearchPartyDod(), data));

        return searchPartyValue;
    }

    private String findTextValueInMap(String valueToFind, Map<String, JsonNode> mapToSearch) {
        JsonNode value = findValueInMap(valueToFind, mapToSearch);
        return jsonNodeObjectToText(value);
    }

    private List<JsonNode> findCollectionValueInMap(uk.gov.hmcts.ccd.domain.model.definition.SearchParty searchParty,
                                                    Map<String, JsonNode> mapToSearch) {
        JsonNode valueInMap = findValueInMap(searchParty.getSearchPartyCollectionFieldName(), mapToSearch);

        List<JsonNode> listOfNodes = new ArrayList<>();

        if (valueInMap != null || !valueInMap.isNull()) {
            if (!valueInMap.isArray()) {
                log.warn("GlobalSearch: This is not a collection in "
                    + "the SearchParty tab and it should be. CaseTypeId: {}, Field: {}",
                    searchParty.getCaseTypeId(), valueInMap.textValue());
            }

            for (JsonNode objNode : valueInMap) {
                listOfNodes.add(objNode);
            }
        }

        return listOfNodes;
    }

    private JsonNode findValueInMap(String valueToFind, Map<String, JsonNode> mapToSearch) {
        JsonNode returnValue = null;

        if (valueToFind != null && mapToSearch != null) {
            for (Map.Entry<String, JsonNode> entry : mapToSearch.entrySet()) {
                boolean processedValue = false;

                if (isComplexField(valueToFind, entry.getKey())) {
                    returnValue = getNestedValueAsJsonNode(entry.getValue(), valueToFind);
                    processedValue = true; // i.e. processed as a nested value

                } else if (entry.getKey().equals(valueToFind)) {
                    returnValue = entry.getValue();
                    processedValue = true;
                }

                if (processedValue) {
                    break; // NB: only one break per loop as per S135
                }
            }
        }

        return returnValue;
    }

    /**
     * Splits out the id from the value from the JsonNode.
     */
    private Map<String, JsonNode> getCollectionItemValueFromJsonNode(JsonNode jsonNode) {
        return jsonNode != null
            ? objectMapperService.convertJsonNodeToMap(jsonNode.get(JSON_NODE_VALUE_KEY_NAME)) : null;
    }

    private String jsonNodeObjectToText(JsonNode jsonNode) {
        return jsonNode != null && !jsonNode.isNull() ? jsonNode.textValue() : null;
    }

}
