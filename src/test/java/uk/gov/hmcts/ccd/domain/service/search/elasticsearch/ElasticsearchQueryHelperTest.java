package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ElasticsearchQueryHelperTest {

    private static final String ROLE_CROSS_JURISDICTION_1 = "cross_jurisdiction_1";
    private static final String ROLE_CROSS_JURISDICTION_2 = "cross_jurisdiction_2";

    private static final String CASE_TYPE_A = "MAPPER";
    private static final String CASE_TYPE_B = "Case_Type_B";
    private static final String CASE_TYPE_C = "Case_Type_C";
    private static final String TEXT_FIELD_ID = "TextField";
    private static final String DATE_FIELD_ID = "DateField";
    private static final String COLLECTION_TEXT_FIELD_ID = "CollectionTextField";
    private static final String COLLECTION_DATE_FIELD_ID = "CollectionDateField";
    private static final String JURISDICTION_1 = "JURISDICTION_1";
    private static final String JURISDICTION_2 = "JURISDICTION_2";
    private static final String LAST_MODIFIED_DATE = "LAST_MODIFIED_DATE";
    private static final FieldTypeDefinition TEXT_FIELD_TYPE = aFieldType().withType("Text").withId("Text").build();
    private static final FieldTypeDefinition DATE_FIELD_TYPE = aFieldType().withType("Date").withId("Date").build();
    private static final FieldTypeDefinition COLLECTION_TEXT_FIELD_TYPE = aFieldType().withType("Collection")
        .withId("Collection").withCollectionFieldType(TEXT_FIELD_TYPE).build();
    private static final FieldTypeDefinition COLLECTION_DATE_FIELD_TYPE = aFieldType().withType("Collection")
        .withId("Collection").withCollectionFieldType(DATE_FIELD_TYPE).build();
    private static final CommonField JURISDICTION_FIELD =
        newCaseField().withMetadata(true).withId(JURISDICTION_1).build();
    private static final CommonField LAST_MODIFIED_DATE_FIELD =
        newCaseField().withMetadata(true).withId(LAST_MODIFIED_DATE).build();
    private static final CommonField TEXT_FIELD =
        newCaseField().withId(TEXT_FIELD_ID).withFieldType(TEXT_FIELD_TYPE).build();
    private static final CommonField DATE_FIELD =
        newCaseField().withId(DATE_FIELD_ID).withFieldType(DATE_FIELD_TYPE).build();
    private static final CommonField COLLECTION_TEXT_FIELD = newCaseField().withId(COLLECTION_TEXT_FIELD_ID)
        .withFieldType(COLLECTION_TEXT_FIELD_TYPE).build();
    private static final CommonField COLLECTION_DATE_FIELD = newCaseField().withId(COLLECTION_DATE_FIELD_ID)
        .withFieldType(COLLECTION_DATE_FIELD_TYPE).build();

    @InjectMocks
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private ObjectMapperService objectMapperService;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapperES = new ObjectMapper();

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Nested
    @DisplayName("getCaseTypesAvailableToUser")
    class GetCaseTypesAvailableToUser {

        @BeforeEach
        void setUp() {
            when(applicationParams.getCcdAccessControlCrossJurisdictionRoles()).thenReturn(
                newArrayList(ROLE_CROSS_JURISDICTION_1, ROLE_CROSS_JURISDICTION_2)
            );
        }

        @DisplayName("should return all case types if user has a cross jurisdiction role")
        @Test
        void shouldReturnAllCaseTypesIfUserHasCrossJurisdictionRole() {

            // ARRANGE
            when(userRepository.anyRoleEqualsAnyOf(anyList())).thenReturn(true);
            when(caseDefinitionRepository.getAllCaseTypesIDs()).thenReturn(
                newArrayList(CASE_TYPE_A, CASE_TYPE_B, CASE_TYPE_C));

            // ACT
            List<String> output  = elasticsearchQueryHelper.getCaseTypesAvailableToUser();

            // ASSERT
            assertAll(
                () -> verify(userRepository).anyRoleEqualsAnyOf(
                    newArrayList(ROLE_CROSS_JURISDICTION_1, ROLE_CROSS_JURISDICTION_2)
                ),
                () -> verify(caseDefinitionRepository).getAllCaseTypesIDs(),
                () -> assertThat(output, is(newArrayList(CASE_TYPE_A, CASE_TYPE_B, CASE_TYPE_C)))
            );
        }

        @DisplayName("should return user's jurisdiction case types if user has no cross jurisdiction role")
        @Test
        void shouldReturnUsersJurisdictionCaseTypesIfUserHasNoCrossJurisdictionRole() {

            // ARRANGE
            when(userRepository.anyRoleEqualsAnyOf(anyList())).thenReturn(false);
            when(userRepository.getCaseworkerUserRolesJurisdictions()).thenReturn(
                newArrayList(JURISDICTION_1, JURISDICTION_2));
            when(caseDefinitionRepository.getCaseTypesIDsByJurisdictions(anyList())).thenReturn(
                newArrayList(CASE_TYPE_B, CASE_TYPE_C));

            // ACT
            List<String> output  = elasticsearchQueryHelper.getCaseTypesAvailableToUser();

            // ASSERT
            assertAll(
                () -> verify(userRepository).anyRoleEqualsAnyOf(
                    newArrayList(ROLE_CROSS_JURISDICTION_1, ROLE_CROSS_JURISDICTION_2)
                ),
                () -> verify(userRepository).getCaseworkerUserRolesJurisdictions(),
                () -> verify(caseDefinitionRepository).getCaseTypesIDsByJurisdictions(
                    newArrayList(JURISDICTION_1, JURISDICTION_2)
                ),
                () -> assertThat(output, is(newArrayList(CASE_TYPE_B, CASE_TYPE_C)))
            );
        }

    }

    @Nested
    @DisplayName("validateAndConvertRequest")
    class ValidateAndConvertRequest {

        @BeforeEach
        void setUp() {
            when(applicationParams.getSearchBlackList()).thenReturn(newArrayList("query_string", "runtime_mappings"));
            doAnswer(invocation -> objectMapperES.readValue((String) invocation.getArgument(0), ObjectNode.class))
                .when(objectMapperService).convertStringToObject(anyString(), any());
            when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE_A);
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(JURISDICTION_1))
                .thenReturn(Optional.of(JURISDICTION_FIELD));
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(LAST_MODIFIED_DATE))
                .thenReturn(Optional.of(LAST_MODIFIED_DATE_FIELD));
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(TEXT_FIELD_ID))
                .thenReturn(Optional.of(TEXT_FIELD));
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(DATE_FIELD_ID))
                .thenReturn(Optional.of(DATE_FIELD));
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(COLLECTION_TEXT_FIELD_ID))
                .thenReturn(Optional.of(COLLECTION_TEXT_FIELD));
            when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(COLLECTION_DATE_FIELD_ID))
                .thenReturn(Optional.of(COLLECTION_DATE_FIELD));
        }

        @Test
        void shouldConvertNativeQueryToElasticSearchRequest() {
            String searchRequest
                = "{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"}]}";

            ElasticsearchRequest elasticsearchRequest
                = elasticsearchQueryHelper.validateAndConvertRequest(searchRequest);

            assertAll(
                () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(), is(searchRequest))
            );
        }

        @Test
        void shouldConvertWrappedQueryToElasticSearchRequest() {
            String searchRequest
                = "{\"native_es_query\":{\"query\":{}},\"supplementary_data\":[\"Field1\",\"Field2\"]}}";

            ElasticsearchRequest elasticsearchRequest
                = elasticsearchQueryHelper.validateAndConvertRequest(searchRequest);

            assertAll(
                () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(), is("{\"query\":{}}")),
                () -> assertThat(elasticsearchRequest.hasRequestedSupplementaryData(), is(true))
            );
        }

        @Test
        void shouldRejectBlacklistedSearchQueriesWithQueryString() {
            String searchRequest = blacklistedQueryWithQueryString();

            BadSearchRequest exception = assertThrows(BadSearchRequest.class,
                () -> elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

            assertAll(
                () -> assertThat(exception.getMessage(), is("Query of type 'query_string' is not allowed"))
            );
        }

        @Test
        void shouldRejectBlacklistedSearchQueriesWithRuntimeMappings() {
            String searchRequest = blacklistedQueryWithRuntimeMappings();

            BadSearchRequest exception = assertThrows(BadSearchRequest.class,
                () -> elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

            assertAll(
                () -> assertThat(exception.getMessage(), is("Query of type 'runtime_mappings' is not allowed"))
            );
        }

        @Test
        void shouldErrorWhenSupplementaryDataIsNotAnArray() {
            String searchRequest
                = "{\"native_es_query\":{\"query\":{}},\"supplementary_data\":{\"object\":\"value\"}}}";

            BadSearchRequest exception = assertThrows(BadSearchRequest.class, () ->
                elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

            assertAll(
                () -> MatcherAssert.assertThat(exception.getMessage(), is("Requested supplementary_data must be an"
                    + " array of text fields."))
            );
        }

        @Test
        void shouldErrorWhenSupplementaryDataIsAnArrayOfNonTextFields() {
            String searchRequest
                = "{\"native_es_query\":{\"query\":{}},\"supplementary_data\":[{\"array\":\"object\"}]}}";

            BadSearchRequest exception = assertThrows(BadSearchRequest.class, () ->
                elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

            assertAll(
                () -> MatcherAssert.assertThat(exception.getMessage(), is("Requested supplementary_data must be an"
                    + " array of text fields."))
            );
        }

        @Test
        void testBadRequestThrowsException() {
            doThrow(new ServiceException("Unable to map JSON string to object"))
                .when(objectMapperService).convertStringToObject(anyString(), any());
            String searchRequest
                = "{\"native_es_query\":{\"query\":,{}},\"supplementary_data\":[{\"array\":\"object\"}]}}";

            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

            assertAll(
                () -> MatcherAssert.assertThat(exception.getMessage(), is("Request requires correctly formatted JSON, "
                    + "Unable to map JSON string to object")));
        }


        private String blacklistedQueryWithQueryString() {
            return "{  \n"
                + "   \"query\":{  \n"
                + "      \"bool\":{  \n"
                + "         \"must\":[  \n"
                + "            {  \n"
                + "               \"simple_query_string\":{  \n"
                + "                  \"query\":\"isde~2\"\n"
                + "               }\n"
                + "            },\n"
                + "            {  \n"
                + "               \"query_string\":{  \n"
                + "                  \"query\":\"isde~2\"\n"
                + "               }\n"
                + "            },\n"
                + "            {  \n"
                + "               \"range\":{  \n"
                + "                  \"data.ComplexField.ComplexNestedField.NestedNumberField\":{  \n"
                + "                     \"lt\":\"91\"\n"
                + "                  }\n"
                + "               }\n"
                + "            }\n"
                + "         ]\n"
                + "      }\n"
                + "   }\n"
                + "}";
        }

        private String blacklistedQueryWithRuntimeMappings() {
            return "{\n"
                + "  \"runtime_mappings\": {\n"
                + "    \"day_of_week\": {\n"
                + "      \"type\": \"keyword\",\n"
                + "      \"script\": {\n"
                + "        \"source\": \"emit(doc['@timestamp'].value.dayOfWeekEnum\"\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"aggs\": {\n"
                + "    \"day_of_week\": {\n"
                + "      \"terms\": {\n"
                + "        \"field\": \"day_of_week\"\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";
        }

    }

}
