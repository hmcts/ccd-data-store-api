package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.jooq.lambda.function.Functions.not;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER)
@Slf4j
public class AuthorisedCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "AuthorisedCaseSearchOperation";
    private static final String SEARCH_ALIAS_CASE_FIELD_PATH_SEPARATOR_REGEX = "\\.";
    private static final String JSON_PATH_COLLECTION_FIELD_INDICATOR = "[*]";
    private static final String JSON_PATH_ROOT_ELEMENT_PREFIX = "$.";

    private final CaseSearchOperation caseSearchOperation;
    private final AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    private final AccessControlService accessControlService;
    private final ObjectMapperService objectMapperService;
    private final CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AuthorisedCaseSearchOperation(
        @Qualifier("classified") CaseSearchOperation caseSearchOperation,
        AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService,
        AccessControlService accessControlService,
        ObjectMapperService objectMapperService,
        CaseDataAccessControl caseDataAccessControl) {

        this.caseSearchOperation = caseSearchOperation;
        this.authorisedCaseDefinitionDataService = authorisedCaseDefinitionDataService;
        this.accessControlService = accessControlService;
        this.objectMapperService = objectMapperService;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    @Override
    public CaseSearchResult execute(CrossCaseTypeSearchRequest searchRequest, boolean dataClassification) {
        List<CaseTypeDefinition> authorisedCaseTypes = getAuthorisedCaseTypes(searchRequest);
        CrossCaseTypeSearchRequest authorisedSearchRequest =
            createAuthorisedSearchRequest(authorisedCaseTypes, searchRequest);

        return searchCasesAndFilterFieldsByAccess(authorisedCaseTypes, authorisedSearchRequest, dataClassification);
    }

    private List<CaseTypeDefinition> getAuthorisedCaseTypes(CrossCaseTypeSearchRequest searchRequest) {
        return searchRequest.getCaseTypeIds()
            .stream()
            .map(caseTypeId ->
                authorisedCaseDefinitionDataService.getAuthorisedCaseType(caseTypeId, CAN_READ).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private CrossCaseTypeSearchRequest createAuthorisedSearchRequest(List<CaseTypeDefinition> authorisedCaseTypes,
                                                                     CrossCaseTypeSearchRequest originalSearchRequest) {
        List<String> authorisedCaseTypeIds =
            authorisedCaseTypes.stream().map(CaseTypeDefinition::getId).collect(Collectors.toList());

        // clone CCT search request object :: then replace case type list
        return new CrossCaseTypeSearchRequest.Builder(originalSearchRequest)
            .withCaseTypes(authorisedCaseTypeIds)
            // NB: preserve original MultiCaseType processing instruction
            .withMultiCaseTypeSearch(originalSearchRequest.isMultiCaseTypeSearch())
            .build();
    }

    private CaseSearchResult searchCasesAndFilterFieldsByAccess(List<CaseTypeDefinition> authorisedCaseTypes,
                                                                CrossCaseTypeSearchRequest authorisedSearchRequest,
                                                                boolean dataClassification) {
        if (authorisedCaseTypes.isEmpty()) {
            return CaseSearchResult.EMPTY;
        }

        CaseSearchResult result = caseSearchOperation.execute(authorisedSearchRequest, dataClassification);
        filterCaseDataByCaseType(authorisedCaseTypes, result.getCases(), authorisedSearchRequest, dataClassification);

        return result;
    }

    private void filterCaseDataByCaseType(List<CaseTypeDefinition> authorisedCaseTypes,
                                          List<CaseDetails> cases,
                                          CrossCaseTypeSearchRequest authorisedSearchRequest,
                                          boolean dataClassification) {
        Map<String, CaseTypeDefinition> caseTypeIdByCaseType = authorisedCaseTypes
            .stream()
            .collect(Collectors.toMap(CaseTypeDefinition::getId, Function.identity()));

        cases.stream()
            .filter(caseDetails -> caseTypeIdByCaseType.containsKey(caseDetails.getCaseTypeId()))
            .forEach(caseDetails -> filterCaseData(caseTypeIdByCaseType.get(caseDetails.getCaseTypeId()),
                                                   caseDetails,
                                                   authorisedSearchRequest,
                                                   dataClassification));
    }

    private void filterCaseData(CaseTypeDefinition authorisedCaseType,
                                CaseDetails caseDetails,
                                CrossCaseTypeSearchRequest authorisedSearchRequest,
                                boolean dataClassification) {
        filterCaseDataByAclAccess(authorisedCaseType, caseDetails);
        filterCaseDataForMultiCaseTypeSearch(authorisedSearchRequest, authorisedCaseType, caseDetails);
    }

    private void filterCaseDataByAclAccess(CaseTypeDefinition authorisedCaseType, CaseDetails caseDetails) {
        log.debug("Case Reference={}, caseTypeId={}", caseDetails.getReference(), caseDetails.getCaseTypeId());
        JsonNode caseData = caseDataToJsonNode(caseDetails);
        JsonNode accessFilteredData =
            accessControlService.filterCaseFieldsByAccess(caseData, authorisedCaseType.getCaseFieldDefinitions(),
                                                            getAccessProfiles(caseDetails),
                                                            CAN_READ, false);
        caseDetails.setData(jsonNodeToCaseData(accessFilteredData));
    }

    /**
     * Filters the case data to the aliases that were passed in the _source filter of the search request.
     * For e.g. if the case data is
     * "case_data": {
     *   "PersonFirstName" : "J",
     *   "PersonLastName": "Baker",
     *   "PersonAddress": {
     *     "city": "London",
     *     "postcode": "W4"
     *   }
     * }
     * and the source filter is
     * "_source": ["alias.lastName", "alias.postcode"]
     * where alias.lastName = case_data.PersonLastName and alias.postcode = case_data.PersonAddress.postcode
     * the case data will be filtered and transformed to
     * "case_data": {
     *   "lastName": "Baker",
     *   "postcode": "W4",
     * }
     * If no source filter was passed then this will remove case data and return only metadata.
     */
    private void filterCaseDataForMultiCaseTypeSearch(CrossCaseTypeSearchRequest searchRequest,
                                                      CaseTypeDefinition authorisedCaseType,
                                                      CaseDetails caseDetails) {
        if (searchRequest.isMultiCaseTypeSearch() && caseDetails.getData() != null
            // NB: bypass MultiCaseType CaseData filters if using a single search index (required for GlobalSearch)
            && searchRequest.getSearchIndex().isEmpty()) {

            JsonNode caseData = caseDataToJsonNode(caseDetails);
            String caseDataJson = caseData.toString();
            JsonNode filteredMultiCaseTypeSearchData = objectMapperService.createEmptyJsonNode();

            authorisedCaseType.getSearchAliasFields()
                .stream()
                .filter(searchRequest::hasAliasField)
                .forEach(searchAliasField -> findCaseFieldPathInCaseData(authorisedCaseType,
                                                                        caseDataJson,
                                                                        searchAliasField.getCaseFieldPath())
                    .filter(not(JsonNode::isMissingNode))
                    .ifPresent(jsonNode ->
                        ((ObjectNode) filteredMultiCaseTypeSearchData).set(searchAliasField.getId(), jsonNode)));

            caseDetails.setData(jsonNodeToCaseData(filteredMultiCaseTypeSearchData));
        }
    }

    private Optional<JsonNode> findCaseFieldPathInCaseData(CaseTypeDefinition caseType,
                                                           String caseDataJson,
                                                           String path) {
        try {
            String fieldPath = JSON_PATH_ROOT_ELEMENT_PREFIX + sanitiseCollectionFieldInPath(caseType, path);
            return of(ofNullable(JsonPath.parse(caseDataJson).read(fieldPath, JsonNode.class))
                .orElse(NullNode.getInstance()));
        } catch (PathNotFoundException e) {
            log.warn("Case field path not found in case data. {}", e.getMessage());
            return of(MissingNode.getInstance());
        }
    }

    /**
     * Collection case field has a different syntax when looking up in the case data.
     * The case field path is stored as `collectionField.value` and when looking up this field, it needs to be
     * looked up as collectionField[*].value
     */
    private String sanitiseCollectionFieldInPath(CaseTypeDefinition caseType, String path) {
        String caseFieldId = getCaseFieldFromPath(path);
        if (path != null && caseType.isCaseFieldACollection(caseFieldId)) {
            return path.replaceAll(caseFieldId, caseFieldId + JSON_PATH_COLLECTION_FIELD_INDICATOR);
        }

        return path;
    }

    /**
     * Case field id is the first element in the path separated by dots
     * e.g. collectionField.value.parentField.childField
     */
    private String getCaseFieldFromPath(String path) {
        return path == null ? "" : path.split(SEARCH_ALIAS_CASE_FIELD_PATH_SEPARATOR_REGEX)[0];
    }

    private Set<AccessProfile> getAccessProfiles(CaseDetails caseDetails) {
        return caseDataAccessControl.generateAccessProfilesByCaseDetails(caseDetails);
    }

    private JsonNode caseDataToJsonNode(CaseDetails caseDetails) {
        return objectMapperService.convertObjectToJsonNode(caseDetails.getData());
    }

    private Map<String, JsonNode> jsonNodeToCaseData(JsonNode jsonNode) {
        return objectMapperService.convertJsonNodeToMap(jsonNode);
    }

}
