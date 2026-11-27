package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
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
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeSearchInputProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.TEXT;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class CriteriaInputCompatibilityTest {
    private static final String CASE_TYPE_ID = "CaseType";
    private static final String FIELD_ID = "documents";
    private static final String CHILD_ID = "reference";
    private static final String CHILD_DCP = "#KEY(Value)";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SearchInputField searchField = input(new SearchInputField());
    private final WorkbasketInputField workbasketField = input(new WorkbasketInputField());
    private CaseDefinitionRepository caseRepository;
    private CaseTypeDefinition caseType;
    private CaseFieldDefinition root;
    private CaseFieldDefinition child;
    private DefaultGetCriteriaOperation operation;

    @BeforeEach
    void setUp() {
        child = newCaseField().withId(CHILD_ID)
            .withFieldType(aFieldType().withId(TEXT).withType(TEXT).build())
            .withDisplayContextParameter(CHILD_DCP).build();
        child.setMetadata(true);
        root = newCaseField().withId(FIELD_ID)
            .withFieldType(aFieldType().withId("Documents").withType(COLLECTION)
                .withCollectionFieldType(aFieldType().withId("DocumentDetails").withType(COMPLEX)
                    .withComplexField(child).build()).build())
            .withDisplayContextParameter("#KEY(Root)").build();
        caseType = new CaseTypeDefinition();
        caseType.setId(CASE_TYPE_ID);
        caseType.setCaseFieldDefinitions(List.of(root));

        SearchInputFieldsDefinition searchInputs = new SearchInputFieldsDefinition();
        searchInputs.setFields(List.of(searchField));
        WorkbasketInputFieldsDefinition workbasketInputs = new WorkbasketInputFieldsDefinition();
        workbasketInputs.setFields(List.of(workbasketField));
        caseRepository = mock(CaseDefinitionRepository.class);
        UIDefinitionRepository uiRepository = mock(UIDefinitionRepository.class);
        when(caseRepository.getCaseType(CASE_TYPE_ID)).thenReturn(caseType);
        when(uiRepository.getSearchInputFieldDefinitions(CASE_TYPE_ID)).thenReturn(searchInputs);
        when(uiRepository.getWorkbasketInputDefinitions(CASE_TYPE_ID)).thenReturn(workbasketInputs);
        operation = new DefaultGetCriteriaOperation(uiRepository, caseRepository);
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldRetainResolvedPropertiesWithoutChangingDefinitionOrRepeatingLookup(CriteriaType criteriaType) {
        JsonNode before = mapper.valueToTree(caseType);
        CriteriaInput result = resolve(criteriaType);
        assertSoftly(softly -> {
            softly.assertThat(result.getDataType()).isEqualTo("collection");
            softly.assertThat(result.getField().getType()).isSameAs(child.getFieldTypeDefinition());
            softly.assertThat(result.getDisplayContextParameter()).isEqualTo(CHILD_DCP);
            softly.assertThat(result.getField().getShowCondition()).isEqualTo("visible=\"Yes\"");
            softly.assertThat(result.getField().isMetadata()).isTrue();
            softly.assertThat(result.buildCaseFieldId()).isEqualTo(FIELD_ID + "." + CHILD_ID);
            softly.assertThat(mapper.<JsonNode>valueToTree(caseType)).isEqualTo(before);
        });
        verify(caseRepository).getCaseType(CASE_TYPE_ID);
        verifyNoMoreInteractions(caseRepository);
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldIgnoreInternalClassificationWhenReadingLegacyJson(CriteriaType criteriaType) throws Exception {
        CriteriaInput original = resolve(criteriaType);
        assertThat(original.getDataType()).isEqualTo("collection");
        ObjectNode json = mapper.valueToTree(original);
        assertThat(json.has("dataType")).isFalse();
        json.put("dataType", "complex");
        CriteriaInput restored = mapper.treeToValue(json, original.getClass());
        assertSoftly(softly -> {
            softly.assertThat(restored.getDataType()).isNull();
            softly.assertThat(restored.getField().getType().getType()).isEqualTo(TEXT);
            softly.assertThat(restored.buildCaseFieldId()).isEqualTo(original.buildCaseFieldId());
        });
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldClassifyCollectionOfPrimitiveValues(CriteriaType criteriaType) {
        root.setFieldTypeDefinition(aFieldType().withType(COLLECTION)
            .withCollectionFieldType(aFieldType().withId(TEXT).withType(TEXT).build()).build());
        searchField.setCaseFieldPath(null);
        workbasketField.setCaseFieldPath(null);
        CriteriaInput result = resolve(criteriaType);
        assertThat(result.getDataType()).isEqualTo("collection");
        assertThat(result.getField().getType().getType()).isEqualTo(COLLECTION);
        assertThat(result.getField().getType().getCollectionFieldTypeDefinition().getType()).isEqualTo(TEXT);
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldPreserveNullClassificationForWholeCollectionWithoutElementType(CriteriaType criteriaType) {
        root.setFieldTypeDefinition(aFieldType().withType(COLLECTION).build());
        searchField.setCaseFieldPath(null);
        workbasketField.setCaseFieldPath(null);
        assertThat(resolve(criteriaType).getDataType()).isNull();
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldTolerateMissingFieldType(CriteriaType criteriaType) {
        root.setFieldTypeDefinition(null);
        searchField.setCaseFieldPath(null);
        workbasketField.setCaseFieldPath(null);
        CriteriaInput result = resolve(criteriaType);
        assertThat(result.getDataType()).isNull();
        assertThat(result.getField().getType()).isNull();
    }

    @ParameterizedTest
    @EnumSource(CriteriaType.class)
    void shouldTolerateMissingTypeName(CriteriaType criteriaType) {
        root.setFieldTypeDefinition(new FieldTypeDefinition());
        searchField.setCaseFieldPath(null);
        workbasketField.setCaseFieldPath(null);
        assertThat(resolve(criteriaType).getDataType()).isNull();
    }

    @ParameterizedTest(name = "{0} preserves {1} processing for collection children")
    @MethodSource("dateInputs")
    void shouldPreserveDateProcessing(CriteriaType criteriaType, String type, String format,
                                     String enteredValue, String expectedValue) {
        child.getFieldTypeDefinition().setId(type);
        child.getFieldTypeDefinition().setType(type);
        child.setDisplayContextParameter("#DATETIMEENTRY(" + format + ")");
        DateTimeSearchInputProcessor processor =
            new DateTimeSearchInputProcessor(new DateTimeFormatParser(), operation);
        String path = FIELD_ID + ".value." + CHILD_ID;
        Map<String, String> result = processor.executeQueryParams(
            criteriaType == SEARCH ? null : criteriaType.name(), new MetaData(CASE_TYPE_ID, "Jurisdiction"),
            Map.of(path, enteredValue));
        assertThat(result).containsExactlyEntriesOf(Map.of(path, expectedValue));
    }

    private CriteriaInput resolve(CriteriaType criteriaType) {
        return operation.execute(CASE_TYPE_ID, CAN_READ, criteriaType).get(0);
    }

    private static Stream<Arguments> dateInputs() {
        return Stream.of(SEARCH, WORKBASKET).flatMap(type -> Stream.of(
            Arguments.of(type, DATE, "dd/MM/yyyy", "20/11/2026", "2026-11-20"),
            Arguments.of(type, DATETIME, "dd/MM/yyyy HH:mm", "20/11/2026 14:35", "2026-11-20T14:35:00.000")
        ));
    }

    private static <T extends CriteriaField> T input(T input) {
        input.setCaseFieldId(FIELD_ID);
        input.setCaseFieldPath(CHILD_ID);
        input.setLabel("Document reference");
        input.setDisplayOrder(1);
        input.setRole("caseworker");
        input.setShowCondition("visible=\"Yes\"");
        return input;
    }
}
