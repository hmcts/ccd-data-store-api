package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.SortOrderField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.SEARCH;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class ElasticsearchSortServiceTest {

    private static final String QUERY_STRING = "{\"query\":{}}";
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private static final String CASE_TYPE_A = "MAPPER";
    private static final String TEXT_FIELD_ID = "TextField";
    private static final String DATE_FIELD_ID = "DateField";
    private static final String COLLECTION_TEXT_FIELD_ID = "CollectionTextField";
    private static final String COLLECTION_DATE_FIELD_ID = "CollectionDateField";
    private static final String JURISDICTION = "JURISDICTION";
    private static final String LAST_MODIFIED_DATE = "LAST_MODIFIED_DATE";
    private static final FieldTypeDefinition TEXT_FIELD_TYPE = aFieldType().withType("Text").withId("Text").build();
    private static final FieldTypeDefinition DATE_FIELD_TYPE = aFieldType().withType("Date").withId("Date").build();
    private static final FieldTypeDefinition COLLECTION_TEXT_FIELD_TYPE = aFieldType().withType("Collection")
        .withId("Collection").withCollectionFieldType(TEXT_FIELD_TYPE).build();
    private static final FieldTypeDefinition COLLECTION_DATE_FIELD_TYPE = aFieldType().withType("Collection")
        .withId("Collection").withCollectionFieldType(DATE_FIELD_TYPE).build();
    private static final CommonField JURISDICTION_FIELD = newCaseField().withMetadata(true).withId(JURISDICTION)
        .build();
    private static final CommonField LAST_MODIFIED_DATE_FIELD = newCaseField().withMetadata(true)
        .withId(LAST_MODIFIED_DATE).build();
    private static final CommonField TEXT_FIELD = newCaseField().withId(TEXT_FIELD_ID).withFieldType(TEXT_FIELD_TYPE)
        .build();
    private static final CommonField DATE_FIELD = newCaseField().withId(DATE_FIELD_ID).withFieldType(DATE_FIELD_TYPE)
        .build();
    private static final CommonField COLLECTION_TEXT_FIELD = newCaseField().withId(COLLECTION_TEXT_FIELD_ID)
        .withFieldType(COLLECTION_TEXT_FIELD_TYPE).build();
    private static final CommonField COLLECTION_DATE_FIELD = newCaseField().withId(COLLECTION_DATE_FIELD_ID)
        .withFieldType(COLLECTION_DATE_FIELD_TYPE).build();

    @InjectMocks
    private ElasticsearchSortService elasticsearchSortService;

    @Spy
    private ObjectMapper objectMapper;
    @Mock
    private ObjectMapperService objectMapperService;
    @Mock
    private SearchQueryOperation searchQueryOperation;
    @Mock
    private CaseTypeService caseTypeService;
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
        doAnswer(invocation -> objectMapperES.readValue((String)invocation.getArgument(0), ObjectNode.class))
            .when(objectMapperService).convertStringToObject(anyString(), any());
        when(caseTypeService.getCaseType(any())).thenReturn(caseTypeDefinition);
        when(caseTypeDefinition.getId()).thenReturn(CASE_TYPE_A);
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(JURISDICTION)))
            .thenReturn(Optional.of(JURISDICTION_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(LAST_MODIFIED_DATE)))
            .thenReturn(Optional.of(LAST_MODIFIED_DATE_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(TEXT_FIELD_ID)))
            .thenReturn(Optional.of(TEXT_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(DATE_FIELD_ID)))
            .thenReturn(Optional.of(DATE_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(COLLECTION_TEXT_FIELD_ID)))
            .thenReturn(Optional.of(COLLECTION_TEXT_FIELD));
        when(caseTypeDefinition.getComplexSubfieldDefinitionByPath(eq(COLLECTION_DATE_FIELD_ID)))
            .thenReturn(Optional.of(COLLECTION_DATE_FIELD));
        when(searchQueryOperation.getSortOrders(any(), any())).thenReturn(sortOrderFields);
    }

    @Test
    void shouldApplyConfiguredSortForMetadataFields() throws JsonProcessingException {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(JURISDICTION).metadata(true).direction(ASC)
            .build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(LAST_MODIFIED_DATE).metadata(true)
            .direction(DESC).build());
        when(elasticsearchMappings.isDefaultTextMetadata(eq(MetaData.CaseField.JURISDICTION.getDbColumnName())))
            .thenReturn(true);
        when(elasticsearchMappings.isDefaultTextMetadata(eq(MetaData.CaseField.LAST_MODIFIED_DATE.getDbColumnName())))
            .thenReturn(false);
        ElasticsearchRequest elasticsearchRequest = new ElasticsearchRequest(objectMapperES.readValue(QUERY_STRING,
            ObjectNode.class));

        elasticsearchSortService.applyConfiguredSort(elasticsearchRequest, CASE_TYPE_A, SEARCH);

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(),
                is("{\"query\":{},\"sort\":[{\"jurisdiction.keyword\":\"ASC\"},{\"last_modified\":\"DESC\"},"
                        + "\"created_date\"]}"))
        );
    }

    @Test
    void shouldApplyConfiguredSortForCaseDataFields() throws JsonProcessingException {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(TEXT_FIELD_ID).direction(ASC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(DATE_FIELD_ID).direction(DESC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(COLLECTION_TEXT_FIELD_ID)
            .direction(DESC).build());
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(COLLECTION_DATE_FIELD_ID)
            .direction(ASC).build());
        when(elasticsearchMappings.isDefaultTextCaseData(eq(TEXT_FIELD_TYPE))).thenReturn(true);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(DATE_FIELD_TYPE))).thenReturn(false);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(COLLECTION_TEXT_FIELD_TYPE))).thenReturn(true);
        when(elasticsearchMappings.isDefaultTextCaseData(eq(COLLECTION_DATE_FIELD_TYPE))).thenReturn(false);
        ElasticsearchRequest elasticsearchRequest =
            new ElasticsearchRequest(objectMapperES.readValue(QUERY_STRING, ObjectNode.class));

        elasticsearchSortService.applyConfiguredSort(elasticsearchRequest, CASE_TYPE_A, WORKBASKET);

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(),
                is("{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"},{\"data.DateField\":\"DESC\"},"
                   + "{\"data.CollectionTextField.value.keyword\":\"DESC\"},"
                    + "{\"data.CollectionDateField.value\":\"ASC\"},\"created_date\"]}"))
        );
    }

    @Test
    void shouldNotApplyConfiguredSortWhenRequestContainsSortOverride() throws JsonProcessingException {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId(JURISDICTION).metadata(true).direction(ASC)
            .build());
        String searchRequest = "{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"}]}";
        ElasticsearchRequest elasticsearchRequest =
            new ElasticsearchRequest(objectMapperES.readValue(searchRequest, ObjectNode.class));

        elasticsearchSortService.applyConfiguredSort(elasticsearchRequest, CASE_TYPE_A, SEARCH);

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(),
                is("{\"query\":{},\"sort\":[{\"data.TextField.keyword\":\"ASC\"},\"created_date\"]}"))
        );
    }

    @Test
    void shouldApplyDefaultSortWhenNoSortsAreConfigured() throws JsonProcessingException {
        String searchRequest = "{\"query\":{}}";
        ElasticsearchRequest elasticsearchRequest =
            new ElasticsearchRequest(objectMapperES.readValue(searchRequest, ObjectNode.class));

        elasticsearchSortService.applyConfiguredSort(elasticsearchRequest, CASE_TYPE_A, SEARCH);

        assertAll(
            () -> assertThat(elasticsearchRequest.getNativeSearchRequest().toString(),
                is("{\"query\":{},\"sort\":[\"created_date\"]}"))
        );
    }

    @Test
    void shouldThrowExceptionWhenSortOrderCaseFieldIsNotInCaseType() throws JsonProcessingException {
        sortOrderFields.add(SortOrderField.sortOrderWith().caseFieldId("UnknownCaseField").direction(ASC).build());
        ElasticsearchRequest elasticsearchRequest =
            new ElasticsearchRequest(objectMapperES.readValue(QUERY_STRING, ObjectNode.class));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> elasticsearchSortService.applyConfiguredSort(elasticsearchRequest, CASE_TYPE_A, WORKBASKET));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("Case field 'UnknownCaseField' does not exist in configuration for case type 'MAPPER'."))
        );
    }
}
