package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.data.user.UserService;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypeOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ElasticsearchQueryHelperTest {

    private static final String QUERY_STRING = "{\"query\":{}}";
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private static final String CASE_TYPE_A = "MAPPER";
    private static final String CASE_TYPE_B = "AAT";
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

    @Spy
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationParams applicationParams;
    @Mock
    private ObjectMapperService objectMapperService;
    @Mock
    private SearchQueryOperation searchQueryOperation;
    @Mock
    private GetCaseTypeOperation getCaseTypeOperation;
    @Mock
    private UserService userService;
    @Mock
    private ElasticsearchMappings elasticsearchMappings;

    private ObjectMapper objectMapperES = new ObjectMapper();

    @Mock
    private CaseTypeDefinition caseTypeDefinition;
    private List<SortOrderField> sortOrderFields;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        sortOrderFields = new ArrayList<>();
        when(applicationParams.getSearchBlackList()).thenReturn(newArrayList("query_string"));
        doAnswer(invocation -> objectMapperES.readValue((String)invocation.getArgument(0), ObjectNode.class))
            .when(objectMapperService).convertStringToObject(anyString(), any());
        when(getCaseTypeOperation.execute(any(), any())).thenReturn(Optional.of(caseTypeDefinition));
        when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE_A);
        when(caseTypeDefinition.getCommonFieldByPath(eq(JURISDICTION))).thenReturn(Optional.of(JURISDICTION_FIELD));
        when(caseTypeDefinition.getCommonFieldByPath(eq(LAST_MODIFIED_DATE))).thenReturn(Optional.of(LAST_MODIFIED_DATE_FIELD));
        when(caseTypeDefinition.getCommonFieldByPath(eq(TEXT_FIELD_ID))).thenReturn(Optional.of(TEXT_FIELD));
        when(caseTypeDefinition.getCommonFieldByPath(eq(DATE_FIELD_ID))).thenReturn(Optional.of(DATE_FIELD));
        when(caseTypeDefinition.getCommonFieldByPath(eq(COLLECTION_TEXT_FIELD_ID))).thenReturn(Optional.of(COLLECTION_TEXT_FIELD));
        when(caseTypeDefinition.getCommonFieldByPath(eq(COLLECTION_DATE_FIELD_ID))).thenReturn(Optional.of(COLLECTION_DATE_FIELD));
        when(searchQueryOperation.getSortOrders(any(), any())).thenReturn(sortOrderFields);
    }

    @Test
    void shouldPrepareBasicSingleCaseTypeRequest() {
        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(Collections.singletonList(CASE_TYPE_A), UseCase.DEFAULT.getReference(), QUERY_STRING);

        assertAll(
            () -> assertThat(request.getCaseTypeIds().size(), is(1)),
            () -> assertThat(request.getCaseTypeIds().get(0), is(CASE_TYPE_A)),
            () -> assertThat(request.isMultiCaseTypeSearch(), is(false)),
            () -> assertThat(request.getAliasFields().isEmpty(), is(true)),
            () -> assertThat(request.getSearchRequestJsonNode().toString(), is("{\"query\":{}}"))
        );
    }

    @Test
    void shouldPrepareBasicCrossCaseTypeRequest() {
        String QUERY_STRING = "{\"_source\":[\"alias.TextAlias\",\"alias.DateAlias\"],\"query\":{}}";

        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(Arrays.asList(CASE_TYPE_A, CASE_TYPE_B), UseCase.ORG_CASES.getReference(), QUERY_STRING);

        assertAll(
            () -> assertThat(request.getCaseTypeIds().size(), is(2)),
            () -> assertThat(request.getCaseTypeIds().get(0), is(CASE_TYPE_A)),
            () -> assertThat(request.getCaseTypeIds().get(1), is(CASE_TYPE_B)),
            () -> assertThat(request.isMultiCaseTypeSearch(), is(true)),
            () -> assertThat(request.getAliasFields().size(), is(2)),
            () -> assertThat(request.getAliasFields().get(0), is("TextAlias")),
            () -> assertThat(request.getAliasFields().get(1), is("DateAlias")),
            () -> assertThat(request.getSearchRequestJsonNode().toString(), is("{\"query\":{}}"))
        );
    }

    @Test
    void shouldPrepareRequestWhenNoCaseTypesSpecified() {
        when(userService.getUserCaseTypes()).thenReturn(Arrays.asList(
            newCaseType().withCaseTypeId(CASE_TYPE_A).build(),
            newCaseType().withCaseTypeId(CASE_TYPE_B).build()
        ));

        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(null, UseCase.ORG_CASES.getReference(), QUERY_STRING);

        assertAll(
            () -> assertThat(request.getCaseTypeIds().size(), is(2)),
            () -> assertThat(request.getCaseTypeIds().get(0), is(CASE_TYPE_A)),
            () -> assertThat(request.getCaseTypeIds().get(1), is(CASE_TYPE_B)),
            () -> assertThat(request.isMultiCaseTypeSearch(), is(true)),
            () -> assertThat(request.getAliasFields().isEmpty(), is(true)),
            () -> assertThat(request.getSearchRequestJsonNode().toString(), is("{\"query\":{}}"))
        );
    }

    @Test
    void shouldApplyConfiguredSortForMetadataFields() {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(JURISDICTION).metadata(true).direction(ASC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(LAST_MODIFIED_DATE).metadata(true).direction(DESC).build());
        when(elasticsearchMappings.isDefaultTextMetadata(eq(MetaData.CaseField.JURISDICTION.getDbColumnName()))).thenReturn(true);
        when(elasticsearchMappings.isDefaultTextMetadata(eq(MetaData.CaseField.LAST_MODIFIED_DATE.getDbColumnName()))).thenReturn(false);

        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(Collections.singletonList(CASE_TYPE_A), UseCase.SEARCH.getReference(), QUERY_STRING);

        assertAll(
            () -> assertThat(request.getSearchRequestJsonNode().toString(),
                is("{\"query\":{},\"sort\":[{\"jurisdiction.keyword\":\"ASC\"},{\"last_modified\":\"DESC\"}]}"))
        );
    }

    @Test
    void shouldApplyConfiguredSortForCaseDataFields() {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(TEXT_FIELD_ID).direction(ASC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(DATE_FIELD_ID).direction(DESC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(COLLECTION_TEXT_FIELD_ID).direction(DESC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(COLLECTION_DATE_FIELD_ID).direction(ASC).build());
        when(elasticsearchMappings.isDefaultTextCaseData(eq(TEXT_FIELD_TYPE))).thenReturn(true);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(DATE_FIELD_TYPE))).thenReturn(false);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(COLLECTION_TEXT_FIELD_TYPE))).thenReturn(true);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(COLLECTION_DATE_FIELD_TYPE))).thenReturn(false);

        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(Collections.singletonList(CASE_TYPE_A), UseCase.WORKBASKET.getReference(), QUERY_STRING);

        assertAll(
            () -> assertThat(request.getSearchRequestJsonNode().toString(),
                is("{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"},{\"data.DateField\":\"DESC\"},"
                   + "{\"data.CollectionTextField.value.keyword\":\"DESC\"},{\"data.CollectionDateField.value\":\"ASC\"}]}"))
        );
    }

    @Test
    void shouldNotApplyConfiguredSortWhenRequestContainsSortOverride() {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(JURISDICTION).metadata(true).direction(ASC).build());
        String queryString = "{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"}]}";

        final CrossCaseTypeSearchRequest request = elasticsearchQueryHelper
            .prepareRequest(Collections.singletonList(CASE_TYPE_A), UseCase.SEARCH.getReference(), queryString);

        assertAll(
            () -> assertThat(request.getSearchRequestJsonNode().toString(),
                is("{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"}]}"))
        );
    }

    @Test
    void shouldRejectRequestsWithInvalidUseCase() {
        final BadSearchRequest exception = assertThrows(BadSearchRequest.class,
            () -> elasticsearchQueryHelper.prepareRequest(Collections.singletonList(CASE_TYPE_A), "INVALID", QUERY_STRING));

        assertAll(
            () -> assertThat(exception.getMessage(), is("The provided use case 'INVALID' is unsupported."))
        );
    }

    @Test
    void shouldRejectBlacklistedSearchQueries() {
        String searchRequest = "{  \n"
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

        BadSearchRequest exception = assertThrows(BadSearchRequest.class,
            () -> elasticsearchQueryHelper.prepareRequest(Collections.singletonList(CASE_TYPE_A), UseCase.DEFAULT.getReference(), searchRequest));

        assertAll(
            () -> assertThat(exception.getMessage(), is("Query of type 'query_string' is not allowed"))
        );
    }

    @Test
    void shouldThrowExceptionWhenSortOrderCaseFieldIsNotInCaseType() {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId("UnknownCaseField").direction(ASC).build());

        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> elasticsearchQueryHelper.prepareRequest(Collections.singletonList(CASE_TYPE_A),
                UseCase.WORKBASKET.getReference(), QUERY_STRING));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("Case field 'UnknownCaseField' does not exist in configuration for case type 'MAPPER'."))
        );
    }
}
