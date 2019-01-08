package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchAliasField;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

class AuthorisedCaseSearchOperationTest {

    private static final String CASE_TYPE_ID_1 = "caseType1";
    private static final String CASE_TYPE_ID_2 = "caseType2";

    @Mock
    private CaseSearchOperation caseSearchOperation;
    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private SecurityClassificationService classificationService;
    @Mock
    private ObjectMapperService objectMapperService;

    private final ObjectNode searchRequestJsonNode = JsonNodeFactory.instance.objectNode();

    @InjectMocks
    private AuthorisedCaseSearchOperation authorisedCaseDetailsSearchOperation;

    private final CaseType caseType = new CaseType();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseType.setId(CASE_TYPE_ID_1);
        searchRequestJsonNode.set(QUERY, mock(ObjectNode.class));
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ)).thenReturn(Optional.of(caseType));
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_2, CAN_READ)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("should filter fields and return search results for valid query")
    void shouldFilterFieldsReturnSearchResults() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID_1);
        CaseSearchResult searchResult = new CaseSearchResult(1L, singletonList(caseDetails));
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class))).thenReturn(searchResult);

        Map<String, JsonNode> unFilteredData = new HashMap<>();
        caseDetails.setData(unFilteredData);
        JsonNode jsonNode = mock(JsonNode.class);
        Set<String> userRoles = new HashSet<>();
        when(userRepository.getUserRoles()).thenReturn(userRoles);
        when(objectMapperService.convertObjectToJsonNode(unFilteredData)).thenReturn(jsonNode);
        CaseType caseType = new CaseType();
        when(accessControlService.filterCaseFieldsByAccess(jsonNode, caseType.getCaseFields(), userRoles, CAN_READ)).thenReturn(jsonNode);
        Map<String, JsonNode> filteredData = new HashMap<>();
        when(objectMapperService.convertJsonNodeToMap(jsonNode)).thenReturn(filteredData);

        CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
            .withSearchRequest(searchRequestJsonNode)
            .build();

        CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest);

        assertAll(
            () -> assertThat(result, is(searchResult)),
            () -> assertThat(caseDetails.getData(), Matchers.is(filteredData)),
            () -> assertThat(result.getTotal(), is(1L)),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_2, CAN_READ),
            () -> verify(caseSearchOperation).execute(any(CrossCaseTypeSearchRequest.class)),
            () -> verify(userRepository).getUserRoles(),
            () -> verify(objectMapperService).convertObjectToJsonNode(unFilteredData),
            () -> verify(accessControlService).filterCaseFieldsByAccess(jsonNode, caseType.getCaseFields(), userRoles, CAN_READ),
            () -> verify(objectMapperService).convertJsonNodeToMap(jsonNode),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }

    @Test
    @DisplayName("should return empty list of cases when user is not authorised to access case type")
    void shouldReturnEmptyCaseList() {
        CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(singletonList(CASE_TYPE_ID_1))
            .withSearchRequest(searchRequestJsonNode)
            .build();
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ)).thenReturn(Optional.empty());

        CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest);

        assertAll(
            () -> assertThat(result.getCases(), hasSize(0)),
            () -> assertThat(result.getTotal(), is(0L)),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
            () -> verifyZeroInteractions(caseSearchOperation),
            () -> verifyZeroInteractions(accessControlService),
            () -> verifyZeroInteractions(userRepository),
            () -> verifyZeroInteractions(classificationService)
        );
    }

    @Test
    @DisplayName("should transform multi-case-type search results to alias names defined in source filter")
    void shouldTransformMultiCaseTypeSearchResults() {
        caseType.setSearchAliasFields(getSearchAliasFields());
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID_1);
        CaseSearchResult searchResult = new CaseSearchResult(1L, singletonList(caseDetails));
        when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class))).thenReturn(searchResult);

        ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
        dataNode.set("firstName", JsonNodeFactory.instance.textNode("Baker"));
        ObjectNode complexNode = JsonNodeFactory.instance.objectNode();
        complexNode.set("postcode", JsonNodeFactory.instance.textNode("W4"));
        dataNode.set("personAddress", complexNode);
        when(objectMapperService.convertObjectToJsonNode(anyMapOf(String.class, JsonNode.class))).thenReturn(dataNode);
        when(objectMapperService.createEmptyJsonNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        Map<String, JsonNode> transformedData = new HashMap<>();
        when(objectMapperService.convertJsonNodeToMap(any(JsonNode.class))).thenReturn(transformedData);

        CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
            .withSearchRequest(searchRequestJsonNode)
            .withMultiCaseTypeSearch(true)
            .withSourceFilterAliasFields(asList("name", "postcode"))
            .build();

        CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest);

        assertAll(
            () -> assertThat(result, is(searchResult)),
            () -> assertThat(caseDetails.getData(), Matchers.is(transformedData)),
            () -> assertThat(result.getTotal(), is(1L)),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_2, CAN_READ),
            () -> verify(caseSearchOperation).execute(any(CrossCaseTypeSearchRequest.class)),
            () -> verify(userRepository).getUserRoles(),
            () -> verify(classificationService).applyClassification(caseDetails)
        );
    }

    private List<SearchAliasField> getSearchAliasFields() {
        SearchAliasField aliasField1 = new SearchAliasField();
        aliasField1.setCaseFieldPath("firstName");
        aliasField1.setId("name");
        SearchAliasField aliasField2 = new SearchAliasField();
        aliasField2.setCaseFieldPath("personAddress.postcode");
        aliasField2.setId("postcode");

        return asList(aliasField1, aliasField2);
    }

}
