package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ElasticsearchQueryHelperTest {

    private static final String CASE_TYPE_A = "MAPPER";
    private static final String TEXT_FIELD_ID = "TextField";
    private static final String DATE_FIELD_ID = "DateField";
    private static final String COLLECTION_TEXT_FIELD_ID = "CollectionTextField";
    private static final String COLLECTION_DATE_FIELD_ID = "CollectionDateField";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String LAST_MODIFIED_DATE = "LAST_MODIFIED_DATE";
    private static final FieldTypeDefinition TEXT_FIELD_TYPE = aFieldType().withType("Text").withId("Text").build();
    private static final FieldTypeDefinition DATE_FIELD_TYPE = aFieldType().withType("Date").withId("Date").build();
    private static final FieldTypeDefinition COLLECTION_TEXT_FIELD_TYPE = aFieldType().withType("Collection").withId("Collection")
        .withCollectionFieldType(TEXT_FIELD_TYPE).build();
    private static final FieldTypeDefinition COLLECTION_DATE_FIELD_TYPE = aFieldType().withType("Collection").withId("Collection")
        .withCollectionFieldType(DATE_FIELD_TYPE).build();
    private static final CommonField JURISDICTION_FIELD = newCaseField().withMetadata(true).withId(JURISDICTION).build();
    private static final CommonField LAST_MODIFIED_DATE_FIELD = newCaseField().withMetadata(true).withId(LAST_MODIFIED_DATE).build();
    private static final CommonField TEXT_FIELD = newCaseField().withId(TEXT_FIELD_ID).withFieldType(TEXT_FIELD_TYPE).build();
    private static final CommonField DATE_FIELD = newCaseField().withId(DATE_FIELD_ID).withFieldType(DATE_FIELD_TYPE).build();
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

    private ObjectMapper objectMapperES = new ObjectMapper();

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(applicationParams.getSearchBlackList()).thenReturn(newArrayList("query_string"));
        doAnswer(invocation -> objectMapperES.readValue((String)invocation.getArgument(0), ObjectNode.class))
            .when(objectMapperService).convertStringToObject(anyString(), any());
        when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE_A);
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(JURISDICTION))).thenReturn(Optional.of(JURISDICTION_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(LAST_MODIFIED_DATE))).thenReturn(Optional.of(LAST_MODIFIED_DATE_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(TEXT_FIELD_ID))).thenReturn(Optional.of(TEXT_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(DATE_FIELD_ID))).thenReturn(Optional.of(DATE_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(COLLECTION_TEXT_FIELD_ID))).thenReturn(Optional.of(COLLECTION_TEXT_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(COLLECTION_DATE_FIELD_ID))).thenReturn(Optional.of(COLLECTION_DATE_FIELD));
    }

    @Test
    void shouldConvertQueryToElasticSearchRequest() {
        String searchRequest = "{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"}]}";

        ElasticsearchRequest elasticsearchRequest = elasticsearchQueryHelper.validateAndConvertRequest(searchRequest);

        assertAll(
            () -> assertThat(elasticsearchRequest.getSearchRequest().toString(), is(searchRequest))
        );
    }

    @Test
    void shouldRejectBlacklistedSearchQueries() {
        String searchRequest = blacklistedQuery();

        BadSearchRequest exception = assertThrows(BadSearchRequest.class,
            () -> elasticsearchQueryHelper.validateAndConvertRequest(searchRequest));

        assertAll(
            () -> assertThat(exception.getMessage(), is("Query of type 'query_string' is not allowed"))
        );
    }

    private String blacklistedQuery() {
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
}
