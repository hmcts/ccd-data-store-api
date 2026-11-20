package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CriteriaField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputField;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputField;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypesOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.endpoint.ui.QueryEndpoint;
import uk.gov.hmcts.ccd.v2.V2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

/**
 * Exercises HTTP mapping, criteria resolution and response serialization with a minimal collection definition.
 * Exercises real criteria access filtering with supplied definitions and access profiles.
 * Definition import, identity authentication and access-profile generation are outside this test's scope.
 */
class UIDefinitionCollectionInputTest {
    private static final String CASE_TYPE_ID = "Benefit";
    private static final String COLLECTION_ID = "sscsDocument";
    private static final String CHILD_ID = "controlNumber";
    private static final String USER_ROLE = "caseworker-sscs-superuser";

    private MockMvc mockMvc;
    private CaseDefinitionRepository caseRepository;
    private UIDefinitionRepository uiRepository;
    private GetCaseTypeOperation getCaseTypeOperation;
    private CaseDataAccessControl caseDataAccessControl;
    private SearchInputFieldsDefinition searchInputs;
    private WorkbasketInputFieldsDefinition workbasketInputs;
    private CaseTypeDefinition caseType;

    private void setUp(String rootType, String elementPath) {
        CaseFieldDefinition controlNumber = newCaseField().withId(CHILD_ID)
            .withFieldType(aFieldType().withId(TEXT).withType(TEXT).build()).build();
        CaseFieldDefinition details = newCaseField().withId("details")
            .withFieldType(aFieldType().withId("DocumentDetails").withType(COMPLEX)
                .withComplexField(controlNumber).build()).build();
        CaseFieldDefinition tags = newCaseField().withId("tags")
            .withFieldType(aFieldType().withType(COLLECTION)
                .withCollectionFieldType(aFieldType().withId(TEXT).withType(TEXT).build()).build()).build();
        CaseFieldDefinition pageCount = newCaseField().withId("pageCount")
            .withFieldType(aFieldType().withId("Number").withType("Number").build()).build();
        CaseFieldDefinition receivedDate = newCaseField().withId("receivedDate")
            .withFieldType(aFieldType().withId("Date").withType("Date").build()).build();
        FieldTypeDefinition scannedDocument = aFieldType().withId("ScannedDocument").withType(COMPLEX)
            .withComplexField(controlNumber).withComplexField(details).withComplexField(tags)
            .withComplexField(pageCount).withComplexField(receivedDate).build();
        FieldTypeDefinition rootFieldType = scannedDocument;
        if (COLLECTION.equalsIgnoreCase(rootType)) {
            rootFieldType = aFieldType().withId("ScannedDocumentCollection").withType(rootType)
                .withCollectionFieldType(scannedDocument).build();
        } else if (!COMPLEX.equals(rootType)) {
            rootFieldType = aFieldType().withId(rootType).withType(rootType).build();
        }
        CaseFieldDefinition documents = newCaseField().withId(COLLECTION_ID)
            .withFieldType(rootFieldType).build();
        caseType = new CaseTypeDefinition();
        caseType.setId(CASE_TYPE_ID);
        caseType.setCaseFieldDefinitions(List.of(documents));

        searchInputs = new SearchInputFieldsDefinition();
        searchInputs.setCaseTypeId(CASE_TYPE_ID);
        searchInputs.setFields(List.of(criteriaInput(new SearchInputField(), elementPath)));
        workbasketInputs = new WorkbasketInputFieldsDefinition();
        workbasketInputs.setCaseTypeId(CASE_TYPE_ID);
        workbasketInputs.setFields(List.of(criteriaInput(new WorkbasketInputField(), elementPath)));

        caseRepository = mock(CaseDefinitionRepository.class);
        uiRepository = mock(UIDefinitionRepository.class);
        when(caseRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(uiRepository.getSearchInputFieldDefinitions(CASE_TYPE_ID)).thenReturn(searchInputs);
        when(uiRepository.getWorkbasketInputDefinitions(CASE_TYPE_ID)).thenReturn(workbasketInputs);

        getCaseTypeOperation = mock(GetCaseTypeOperation.class);
        caseDataAccessControl = mock(CaseDataAccessControl.class);
        when(getCaseTypeOperation.execute(CASE_TYPE_ID, CAN_READ)).thenReturn(Optional.of(caseType));
        when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(CASE_TYPE_ID))
            .thenReturn(Set.of(AccessProfile.builder().accessProfile(USER_ROLE).readOnly(false).build()));
        AuthorisedGetCriteriaOperation authorisedCriteria = new AuthorisedGetCriteriaOperation(
            new DefaultGetCriteriaOperation(uiRepository, caseRepository), getCaseTypeOperation, caseDataAccessControl);
        UIDefinitionController controller = new UIDefinitionController(authorisedCriteria,
            mock(GetBannerOperation.class), mock(GetUserProfileOperation.class),
            mock(GetJurisdictionUiConfigOperation.class));
        QueryEndpoint legacyController = new QueryEndpoint(
            mock(AuthorisedGetCaseViewOperation.class), mock(AuthorisedGetCaseHistoryViewOperation.class),
            mock(GetEventTriggerOperation.class), mock(SearchQueryOperation.class),
            mock(FieldMapSanitizeOperation.class), authorisedCriteria,
            mock(GetCaseTypesOperation.class), mock(GetUserProfileOperation.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller, legacyController).build();
    }

    @ParameterizedTest(name = "{0}: root={3}, path={4}, field type={5}, dataType={6}")
    @MethodSource("inputScenarios")
    void shouldReturnInputTypes(String endpoint, String responseKey, String mediaType, String rootType,
                               String elementPath, String expectedFieldType, String expectedDataType)
        throws Exception {
        setUp(rootType, elementPath);
        MvcResult result = mockMvc.perform(get("/internal/case-types/{caseTypeId}/{endpoint}", CASE_TYPE_ID, endpoint)
                .header(V2.EXPERIMENTAL_HEADER, "true")
                .accept(mediaType))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode inputs = mapper.readTree(responseBody).path(responseKey);
        JsonNode expectedElementPath = mapper.valueToTree(elementPath);
        JsonNode expectedClassification = mapper.valueToTree(expectedDataType);
        assertThat(inputs).hasSize(1);
        JsonNode input = inputs.get(0);
        JsonNode field = input.path("field");
        assertSoftly(softly -> {
            softly.assertThat(field.path("id").asText()).isEqualTo(COLLECTION_ID);
            softly.assertThat(field.path("elementPath")).isEqualTo(expectedElementPath);
            softly.assertThat(field.path("field_type").path("type").asText()).isEqualTo(expectedFieldType);
            softly.assertThat(field.has("rootType")).isFalse();
            softly.assertThat(field.has("dataType")).isFalse();
            softly.assertThat(input.path("label").asText()).isEqualTo("Document control number");
            softly.assertThat(input.path("order").asInt()).isEqualTo(1);
            softly.assertThat(input.has("dataType")).isTrue();
            softly.assertThat(input.path("dataType"))
                .as("Response: %s", responseBody).isEqualTo(expectedClassification);
        });
        verify(caseRepository).getCaseType(CASE_TYPE_ID);
        verifyNoMoreInteractions(caseRepository);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldKeepLegacyResponseShape(String endpoint, String responseKey, String mediaType) throws Exception {
        setUp(COLLECTION, CHILD_ID);
        String legacyEndpoint = "search-inputs".equals(endpoint) ? "inputs" : "work-basket-inputs";
        MvcResult result = mockMvc.perform(get(
                "/aggregated/caseworkers/0/jurisdictions/SSCS/case-types/{caseTypeId}/{endpoint}",
                CASE_TYPE_ID, legacyEndpoint).contentType("application/json"))
            .andExpect(status().isOk()).andReturn();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode inputs = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(inputs).hasSize(1);
        JsonNode input = inputs.get(0);
        JsonNode expected = mapper.readTree("""
            {
              "label": "Document control number",
              "order": 1,
              "role": "caseworker-sscs-superuser",
              "display_context_parameter": null,
              "field": {
                "id": "sscsDocument",
                "elementPath": "controlNumber",
                "metadata": false,
                "show_condition": null,
                "field_type": {
                  "id": "Text",
                  "type": "Text",
                  "min": null,
                  "max": null,
                  "regular_expression": null,
                  "fixed_list_items": [],
                  "complex_fields": [],
                  "collection_field_type": null
                }
              }
            }
            """);
        assertThat(input).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldApplyFallbackOnlyToUnclassifiedInputInMixedResponse(
        String endpoint, String responseKey, String mediaType) throws Exception {
        setUp(COLLECTION, CHILD_ID);
        CaseFieldDefinition scalar = newCaseField().withId("caseReference")
            .withFieldType(aFieldType().withId(TEXT).withType(TEXT).build()).build();
        caseType.setCaseFieldDefinitions(List.of(caseType.getCaseField(COLLECTION_ID).orElseThrow(), scalar));

        SearchInputField scalarSearchInput = criteriaInput(new SearchInputField(), null);
        scalarSearchInput.setCaseFieldId("caseReference");
        searchInputs.setFields(List.of(criteriaInput(new SearchInputField(), CHILD_ID),
            criteriaInput(new SearchInputField(), "details"), scalarSearchInput));
        WorkbasketInputField scalarWorkbasketInput = criteriaInput(new WorkbasketInputField(), null);
        scalarWorkbasketInput.setCaseFieldId("caseReference");
        workbasketInputs.setFields(List.of(criteriaInput(new WorkbasketInputField(), CHILD_ID),
            criteriaInput(new WorkbasketInputField(), "details"), scalarWorkbasketInput));

        JsonNode inputs = responseInputs(endpoint, responseKey, mediaType);
        assertThat(inputs).extracting(
            input -> input.path("field").path("id").asText(),
            input -> input.path("field").path("elementPath"),
            input -> input.path("field").path("field_type").path("type").asText(),
            input -> input.path("dataType")
        ).containsExactly(
            tuple(COLLECTION_ID, JsonNodeFactory.instance.textNode(CHILD_ID), TEXT,
                JsonNodeFactory.instance.textNode("collection")),
            tuple(COLLECTION_ID, JsonNodeFactory.instance.textNode("details"), COMPLEX,
                JsonNodeFactory.instance.textNode("complex")),
            tuple("caseReference", JsonNodeFactory.instance.nullNode(), TEXT, JsonNodeFactory.instance.nullNode())
        );
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldFilterInputsWithoutMatchingProfile(String endpoint, String responseKey, String mediaType)
        throws Exception {
        setUp(COLLECTION, CHILD_ID);
        when(caseDataAccessControl.generateAccessProfilesByCaseTypeId(CASE_TYPE_ID)).thenReturn(Set.of());
        assertEmptyResponse(endpoint, responseKey, mediaType);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldFilterInputsWithoutFieldReadAccess(String endpoint, String responseKey, String mediaType)
        throws Exception {
        setUp(COLLECTION, CHILD_ID);
        CaseTypeDefinition authorisedCaseType = new CaseTypeDefinition();
        authorisedCaseType.setCaseFieldDefinitions(List.of());
        when(getCaseTypeOperation.execute(CASE_TYPE_ID, CAN_READ)).thenReturn(Optional.of(authorisedCaseType));
        assertEmptyResponse(endpoint, responseKey, mediaType);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldReturnNotFoundBeforeReadingDefinitionsWhenCaseTypeIsNotReadable(
        String endpoint, String responseKey, String mediaType) throws Exception {
        setUp(COLLECTION, CHILD_ID);
        when(getCaseTypeOperation.execute(CASE_TYPE_ID, CAN_READ)).thenReturn(Optional.empty());
        mockMvc.perform(get("/internal/case-types/{caseTypeId}/{endpoint}", CASE_TYPE_ID, endpoint)
                .header(V2.EXPERIMENTAL_HEADER, "true").accept(mediaType))
            .andExpect(status().isNotFound());
        verifyNoInteractions(caseRepository, uiRepository, caseDataAccessControl);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldDeduplicateInputsWithoutLosingClassification(String endpoint, String responseKey, String mediaType)
        throws Exception {
        setUp(COLLECTION, CHILD_ID);
        searchInputs.setFields(List.of(criteriaInput(new SearchInputField(), CHILD_ID),
            criteriaInput(new SearchInputField(), CHILD_ID)));
        workbasketInputs.setFields(List.of(criteriaInput(new WorkbasketInputField(), CHILD_ID),
            criteriaInput(new WorkbasketInputField(), CHILD_ID)));
        JsonNode inputs = responseInputs(endpoint, responseKey, mediaType);
        assertThat(inputs).hasSize(1);
        assertThat(inputs.get(0).path("dataType").asText()).isEqualTo("collection");
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldReturnEmptyInputs(String endpoint, String responseKey, String mediaType) throws Exception {
        setUp(COLLECTION, CHILD_ID);
        searchInputs.setFields(List.of());
        workbasketInputs.setFields(List.of());
        assertEmptyResponse(endpoint, responseKey, mediaType);
    }

    @ParameterizedTest
    @MethodSource("inputEndpoints")
    void shouldRejectUnknownChild(String endpoint, String responseKey, String mediaType) throws Exception {
        setUp(COLLECTION, "missing");
        mockMvc.perform(get("/internal/case-types/{caseTypeId}/{endpoint}", CASE_TYPE_ID, endpoint)
                .header(V2.EXPERIMENTAL_HEADER, "true").accept(mediaType))
            .andExpect(status().isBadRequest());
    }

    private void assertEmptyResponse(String endpoint, String responseKey, String mediaType) throws Exception {
        JsonNode inputs = responseInputs(endpoint, responseKey, mediaType);
        assertThat(inputs.isArray()).isTrue();
        assertThat(inputs).isEmpty();
    }

    private JsonNode responseInputs(String endpoint, String responseKey, String mediaType) throws Exception {
        MvcResult result = mockMvc.perform(get("/internal/case-types/{caseTypeId}/{endpoint}", CASE_TYPE_ID, endpoint)
                .header(V2.EXPERIMENTAL_HEADER, "true").accept(mediaType))
            .andExpect(status().isOk()).andReturn();
        return new ObjectMapper().readTree(result.getResponse().getContentAsString()).path(responseKey);
    }

    private static Stream<Arguments> inputScenarios() {
        List<Arguments> scenarios = List.of(
            Arguments.of(COLLECTION, CHILD_ID, TEXT, "collection"),
            Arguments.of(COLLECTION, "pageCount", "Number", "collection"),
            Arguments.of(COLLECTION, "receivedDate", "Date", "collection"),
            Arguments.of(COLLECTION, null, COLLECTION, "collection"),
            Arguments.of(COMPLEX, null, COMPLEX, "complex"),
            Arguments.of(TEXT, null, TEXT, null),
            Arguments.of("Number", null, "Number", null),
            Arguments.of("Date", null, "Date", null),
            Arguments.of("DateTime", null, "DateTime", null),
            Arguments.of(COMPLEX, CHILD_ID, TEXT, null),
            Arguments.of(COMPLEX, "pageCount", "Number", null),
            Arguments.of(COMPLEX, "receivedDate", "Date", null),
            Arguments.of(COMPLEX, "details", COMPLEX, "complex"),
            Arguments.of(COMPLEX, "details." + CHILD_ID, TEXT, null),
            Arguments.of(COMPLEX, "tags", COLLECTION, "collection"),
            Arguments.of(COLLECTION, "", COLLECTION, "collection"),
            Arguments.of("collection", CHILD_ID, TEXT, "collection"),
            Arguments.of(COLLECTION, "details", COMPLEX, "complex"),
            Arguments.of(COLLECTION, "tags", COLLECTION, "collection"),
            Arguments.of(COLLECTION, "details." + CHILD_ID, TEXT, "collection")
        );
        return inputEndpoints().flatMap(endpoint -> scenarios.stream().map(scenario -> Arguments.of(
            endpoint.get()[0], endpoint.get()[1], endpoint.get()[2],
            scenario.get()[0], scenario.get()[1], scenario.get()[2], scenario.get()[3])));
    }

    private static Stream<Arguments> inputEndpoints() {
        return Stream.of(
            Arguments.of("search-inputs", "searchInputs", V2.MediaType.UI_SEARCH_INPUT_DETAILS),
            Arguments.of("work-basket-inputs", "workbasketInputs", V2.MediaType.UI_WORKBASKET_INPUT_DETAILS)
        );
    }

    private static <T extends CriteriaField> T criteriaInput(T input, String elementPath) {
        input.setCaseFieldId(COLLECTION_ID);
        input.setCaseFieldPath(elementPath);
        input.setLabel("Document control number");
        input.setDisplayOrder(1);
        input.setRole(USER_ROLE);
        return input;
    }
}
