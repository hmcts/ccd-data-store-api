package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.JsonPathExtension;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchAliasField;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchIndex;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.QUERY;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@ExtendWith(JsonPathExtension.class)
class AuthorisedCaseSearchOperationTest {

    private static final String CASE_TYPE_ID_1 = "caseType1";
    private static final String CASE_TYPE_ID_2 = "caseType2";
    private static final Long CASE_REFERENCE = 123456L;
    private static final String CASE_REFERENCE_STR = "123456";

    @Mock
    private CaseSearchOperation caseSearchOperation;
    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseDataAccessControl caseDataAccessControl;
    @Mock
    private AccessControlService accessControlService;

    @Mock
    private ObjectMapperService objectMapperService;

    private final ObjectNode searchRequestJsonNode = JsonNodeFactory.instance.objectNode();

    private final ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(searchRequestJsonNode);

    @InjectMocks
    private AuthorisedCaseSearchOperation authorisedCaseDetailsSearchOperation;

    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseTypeDefinition.setId(CASE_TYPE_ID_1);
        searchRequestJsonNode.set(QUERY, mock(ObjectNode.class));
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ))
            .thenReturn(Optional.of(caseTypeDefinition));
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_2, CAN_READ))
            .thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("Single case type search")
    class SingleCaseTypeDefinitionSearch {

        @Test
        @DisplayName("should filter fields and return search results for valid query")
        void shouldFilterFieldsReturnSearchResults() {
            CaseDetails caseDetails = new CaseDetails();
            caseDetails.setReference(CASE_REFERENCE);
            caseDetails.setCaseTypeId(CASE_TYPE_ID_1);
            CaseSearchResult searchResult = new CaseSearchResult(1L, singletonList(caseDetails));
            when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class),
                anyBoolean())).thenReturn(searchResult);

            Map<String, JsonNode> unFilteredData = new HashMap<>();
            caseDetails.setData(unFilteredData);
            JsonNode jsonNode = mock(JsonNode.class);
            Set<String> userRoles = new HashSet<>();
            Set<AccessProfile> accessProfiles = createAccessProfiles(userRoles);

            when(caseDataAccessControl.generateAccessProfilesByCaseDetails(any(CaseDetails.class)))
                .thenReturn(accessProfiles);

            when(objectMapperService.convertObjectToJsonNode(unFilteredData)).thenReturn(jsonNode);
            CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
            when(accessControlService.filterCaseFieldsByAccess(jsonNode,
                caseTypeDefinition.getCaseFieldDefinitions(),
                accessProfiles, CAN_READ, false)).thenReturn(jsonNode);
            Map<String, JsonNode> filteredData = new HashMap<>();
            when(objectMapperService.convertJsonNodeToMap(jsonNode)).thenReturn(filteredData);

            CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();

            CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest, true);

            assertAll(
                () -> assertThat(result, is(searchResult)),
                () -> assertThat(caseDetails.getData(), Matchers.is(filteredData)),
                () -> assertThat(result.getTotal(), is(1L)),
                () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
                () -> verify(caseSearchOperation).execute(any(CrossCaseTypeSearchRequest.class), anyBoolean()),
                () -> verify(caseDataAccessControl).generateAccessProfilesByCaseDetails(any(CaseDetails.class)),
                () -> verify(objectMapperService).convertObjectToJsonNode(unFilteredData),
                () -> verify(accessControlService).filterCaseFieldsByAccess(jsonNode,
                    caseTypeDefinition.getCaseFieldDefinitions(), accessProfiles, CAN_READ, false),
                () -> verify(objectMapperService).convertJsonNodeToMap(jsonNode)
            );
        }

        @Test
        @DisplayName("should return empty list of cases when user is not authorised to access case type")
        void shouldReturnEmptyCaseList() {
            CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(singletonList(CASE_TYPE_ID_1))
                .withSearchRequest(elasticsearchRequest)
                .build();
            when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ))
                .thenReturn(Optional.empty());

            CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest, true);

            assertAll(
                () -> assertThat(result.getCases(), hasSize(0)),
                () -> assertThat(result.getTotal(), is(0L)),
                () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
                () -> verifyZeroInteractions(caseSearchOperation),
                () -> verifyZeroInteractions(accessControlService),
                () -> verifyZeroInteractions(userRepository)
            );
        }

    }

    @Nested
    @DisplayName("Cross case type search")
    class CrossCaseTypeSearch {

        private final CaseDetails caseDetails = new CaseDetails();
        private CaseSearchResult searchResult;
        private final Map<String, JsonNode> transformedData = new HashMap<>();

        @BeforeEach
        void setUp() {
            caseDetails.setCaseTypeId(CASE_TYPE_ID_1);
            caseDetails.setReference(CASE_REFERENCE);
            caseTypeDefinition.setSearchAliasFields(getSearchAliasFields());
            searchResult = new CaseSearchResult(1L, singletonList(caseDetails));
            when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class),
                anyBoolean())).thenReturn(searchResult);
            when(objectMapperService.createEmptyJsonNode()).thenReturn(JsonNodeFactory.instance.objectNode());
            when(objectMapperService.convertJsonNodeToMap(any(JsonNode.class))).thenReturn(transformedData);
        }

        @Test
        @DisplayName("should preserve search index property in execute call if set")
        void shouldPreserveSearchIndexPropertyInExecuteCall() {

            // ARRANGE
            when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class),
                anyBoolean())).thenReturn(searchResult);

            ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
            dataNode.set("firstName", JsonNodeFactory.instance.textNode("Baker"));
            ObjectNode complexNode = JsonNodeFactory.instance.objectNode();
            complexNode.set("postcode", JsonNodeFactory.instance.textNode("W4"));
            dataNode.set("personAddress", complexNode);
            when(objectMapperService.convertObjectToJsonNode(anyMapOf(String.class, JsonNode.class)))
                .thenReturn(dataNode);

            SearchIndex searchIndex = new SearchIndex(
                "my_index_name",
                "my_index_type"
            );

            CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .withMultiCaseTypeSearch(true)
                .withSearchIndex(searchIndex)
                .build();

            // ACT
            CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest, true);

            // ASSERT
            verifyResult(result);

            // ::verify search index has been preserved
            ArgumentCaptor<CrossCaseTypeSearchRequest> searchRequestCaptor
                = ArgumentCaptor.forClass(CrossCaseTypeSearchRequest.class);
            verify(caseSearchOperation).execute(searchRequestCaptor.capture(), anyBoolean());

            CrossCaseTypeSearchRequest actualSearchRequest = searchRequestCaptor.getValue();
            assertThat(actualSearchRequest.getSearchIndex().isPresent(), is(true));
            assertThat(actualSearchRequest.getSearchIndex().get(), is(searchIndex));
        }

        @Test
        @DisplayName("should transform search results to alias names defined in source filter")
        void shouldTransformMultiCaseTypeSearchResults() {
            when(caseSearchOperation.execute(any(CrossCaseTypeSearchRequest.class),
                anyBoolean())).thenReturn(searchResult);

            ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
            dataNode.set("firstName", JsonNodeFactory.instance.textNode("Baker"));
            ObjectNode complexNode = JsonNodeFactory.instance.objectNode();
            complexNode.set("postcode", JsonNodeFactory.instance.textNode("W4"));
            dataNode.set("personAddress", complexNode);
            when(objectMapperService.convertObjectToJsonNode(anyMapOf(String.class, JsonNode.class)))
                .thenReturn(dataNode);

            CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .withMultiCaseTypeSearch(true)
                .withSourceFilterAliasFields(asList("name", "postcode"))
                .build();

            CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest, true);
            verifyResult(result);
        }

        @Test
        @DisplayName("should transform alias field for a collection field in source filter ")
        void shouldTransformAliasFieldForCollection() {
            CaseFieldDefinition collectionField = new CaseFieldDefinition();
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType(FieldTypeDefinition.COLLECTION);
            collectionField.setFieldTypeDefinition(fieldTypeDefinition);
            collectionField.setId("collectionField");
            caseTypeDefinition.getCaseFieldDefinitions().add(collectionField);

            ObjectNode dataNode = JsonNodeFactory.instance.objectNode();
            ObjectNode textNode = JsonNodeFactory.instance.objectNode();
            textNode.set("value", JsonNodeFactory.instance.textNode("some text"));
            ArrayNode collectionNode = JsonNodeFactory.instance.arrayNode();
            collectionNode.add(textNode);
            dataNode.set("collectionField", collectionNode);

            when(objectMapperService.convertObjectToJsonNode(anyMapOf(String.class, JsonNode.class)))
                .thenReturn(dataNode);

            CrossCaseTypeSearchRequest searchRequest = new CrossCaseTypeSearchRequest.Builder()
                .withCaseTypes(asList(CASE_TYPE_ID_1, CASE_TYPE_ID_2))
                .withSearchRequest(elasticsearchRequest)
                .withMultiCaseTypeSearch(true)
                .withSourceFilterAliasFields(asList("collection", "postcode"))
                .build();

            CaseSearchResult result = authorisedCaseDetailsSearchOperation.execute(searchRequest, true);
            verifyResult(result);
        }

        private void verifyResult(CaseSearchResult result) {
            assertAll(
                () -> assertThat(result, is(searchResult)),
                () -> assertThat(caseDetails.getData(), Matchers.is(transformedData)),
                () -> assertThat(result.getTotal(), is(1L)),
                () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_1, CAN_READ),
                () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE_ID_2, CAN_READ),
                () -> verify(caseSearchOperation).execute(any(CrossCaseTypeSearchRequest.class), anyBoolean()),
                () -> verify(caseDataAccessControl).generateAccessProfilesByCaseDetails(any(CaseDetails.class)),
                () -> verify(caseDataAccessControl).generateAccessProfilesByCaseDetails(any(CaseDetails.class))
            );
        }
    }

    private List<SearchAliasField> getSearchAliasFields() {
        SearchAliasField aliasField1 = new SearchAliasField();
        aliasField1.setCaseFieldPath("firstName");
        aliasField1.setId("name");
        SearchAliasField aliasField2 = new SearchAliasField();
        aliasField2.setCaseFieldPath("personAddress.postcode");
        aliasField2.setId("postcode");
        SearchAliasField aliasField3 = new SearchAliasField();
        aliasField3.setCaseFieldPath("collectionField.value");
        aliasField3.setId("collection");
        SearchAliasField aliasField4 = new SearchAliasField();
        aliasField4.setCaseFieldPath("pathNotFound");
        aliasField4.setId("pathNotFound");

        return asList(aliasField1, aliasField2, aliasField3, aliasField4);
    }

    private Set<AccessProfile> createAccessProfiles(Set<String> userRoles) {
        return userRoles.stream()
            .map(userRole -> AccessProfile.builder().readOnly(false)
                .accessProfile(userRole)
                .build())
            .collect(Collectors.toSet());
    }
}
