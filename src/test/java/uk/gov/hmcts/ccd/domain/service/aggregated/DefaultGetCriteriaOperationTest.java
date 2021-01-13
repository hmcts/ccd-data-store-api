package uk.gov.hmcts.ccd.domain.service.aggregated;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputField;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputField;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

@DisplayName("DefaultGetCriteriaOperationTest")
public class DefaultGetCriteriaOperationTest {
    private static final String TEXT_TYPE = "Text";
    private static final String DATE_TYPE = "Date";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";
    private static final String DOB = "DoB";
    private static final String DISPLAY_CONTEXT_PARAMETER = "#KEY(Value)";
    private static final String SHOW_CONDITION = "some show condition";

    private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
    private final CaseFieldDefinition caseFieldDefinition1 = new CaseFieldDefinition();
    private final CaseFieldDefinition caseFieldDefinition2 = new CaseFieldDefinition();
    private final CaseFieldDefinition caseFieldDefinition3 = new CaseFieldDefinition();
    private final CaseFieldDefinition caseFieldDefinition4 = new CaseFieldDefinition();

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    private DefaultGetCriteriaOperation defaultGetCriteriaOperation;

    private CaseFieldDefinition name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE)
        .withType(TEXT_TYPE).build()).build();
    private CaseFieldDefinition surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE)
        .withType(TEXT_TYPE).build()).build();
    private FieldTypeDefinition personFieldTypeDefinition = aFieldType()
        .withId(PERSON)
        .withType(COMPLEX)
        .withComplexField(name)
        .withComplexField(surname)
        .build();
    private CaseFieldDefinition person =
        newCaseField().withId(PERSON).withFieldType(personFieldTypeDefinition).build();
    private CaseFieldDefinition dob =
        newCaseField().withId(DOB).withFieldType(aFieldType().withId(DATE_TYPE).withType(DATE_TYPE).build())
        .withDisplayContextParameter(DISPLAY_CONTEXT_PARAMETER).build();
    private FieldTypeDefinition personFieldType = aFieldType().withId(PERSON).withType(COMPLEX)
        .withComplexField(name).withComplexField(surname).withComplexField(dob).build();

    private FieldTypeDefinition debtorFieldTypeDefinition =
        aFieldType().withId(DEBTOR_DETAILS).withType(COMPLEX).withComplexField(person).build();
    private CaseFieldDefinition debtorDetails =
        newCaseField().withId(DEBTOR_DETAILS).withFieldType(debtorFieldTypeDefinition).build();

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        caseFieldDefinition1.setId("field1");
        caseFieldDefinition1.setFieldTypeDefinition(fieldTypeDefinition);
        caseFieldDefinition2.setId("field2");
        caseFieldDefinition2.setFieldTypeDefinition(fieldTypeDefinition);
        caseFieldDefinition3.setId("field3");
        caseFieldDefinition3.setFieldTypeDefinition(fieldTypeDefinition);
        caseFieldDefinition4.setId("field4");
        caseFieldDefinition4.setFieldTypeDefinition(fieldTypeDefinition);
        caseFieldDefinition4.setMetadata(true);
        caseTypeDefinition.setId("Test case type");
        caseTypeDefinition.setCaseFieldDefinitions(asList(caseFieldDefinition1,
                                                    caseFieldDefinition2, caseFieldDefinition3,
                                                    caseFieldDefinition4, debtorDetails));

        defaultGetCriteriaOperation =
            new DefaultGetCriteriaOperation(uiDefinitionRepository, caseDefinitionRepository);

        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(caseTypeDefinition.getId());
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository)
            .getWorkbasketInputDefinitions(caseTypeDefinition.getId());
        doReturn(generateSearchInput()).when(uiDefinitionRepository)
            .getSearchInputFieldDefinitions(caseTypeDefinition.getId());
    }

    @Test
    void shouldReturnWorkbasketInputs() {
        List<? extends CriteriaInput> workbasketInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(workbasketInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    @Test
    void shouldReturnSearchInputs() {
        List<? extends CriteriaInput> searchInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(4)),
            () -> assertThat(searchInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(searchInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    @Test
    void shouldReturnSearchInputsWithShowCondition() {
        List<? extends CriteriaInput> searchInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, SEARCH);

        assertThat(searchInputs, hasItems(hasProperty("field", hasProperty("showCondition",
            equalTo(SHOW_CONDITION)))));
    }

    @Test
    void shouldReturnWorkbasketInputsWithNullShowCondition() {
        List<? extends CriteriaInput> workbasketInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, WORKBASKET);

        assertThat(workbasketInputs, hasItems(hasProperty("field",
            hasProperty("showCondition", nullValue()))));
    }


    @Test
    void shouldReturnWorkbasketInputsWhenCaseFieldElementPathDefined() {
        doReturn(generateWorkbasketInputWithPathElements()).when(uiDefinitionRepository)
            .getWorkbasketInputDefinitions(caseTypeDefinition.getId());
        List<? extends CriteriaInput> workbasketInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(5)),
            () -> assertThat(workbasketInputs.get(4).getField().getId(), is(DEBTOR_DETAILS)),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getType(),
                is(name.getFieldTypeDefinition().getType())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getId(),
                is(name.getFieldTypeDefinition().getId())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getChildren().size(), is(0))
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCaseFieldNotFoundInCaseTypeForWorkbasketInput() {
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository)
            .getWorkbasketInputDefinitions(caseTypeDefinition.getId());
        caseTypeDefinition.setCaseFieldDefinitions(Collections.emptyList());

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, WORKBASKET));

        assertThat(exception.getMessage(),
            is("CaseField with id=[field1] and path=[null] not found"));
    }

    @Test
    void shouldReturnSearchInputsWhenCaseFieldElementPathDefined() {
        doReturn(generateSearchInputWithPathElements()).when(uiDefinitionRepository)
            .getSearchInputFieldDefinitions(caseTypeDefinition.getId());
        List<? extends CriteriaInput> searchInputs =
            defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(5)),
            () -> assertThat(searchInputs.get(4).getField().getId(), is(DEBTOR_DETAILS)),
            () -> assertThat(searchInputs.get(4).getField().getType().getType(),
                is(name.getFieldTypeDefinition().getType())),
            () -> assertThat(searchInputs.get(4).getField().getType().getId(),
                is(name.getFieldTypeDefinition().getId())),
            () -> assertThat(searchInputs.get(4).getField().getType().getChildren().size(), is(0)));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCaseFieldNotFoundInCaseTypeForSearchInput() {
        doReturn(generateSearchInput()).when(uiDefinitionRepository)
            .getSearchInputFieldDefinitions(caseTypeDefinition.getId());
        caseTypeDefinition.setCaseFieldDefinitions(Collections.emptyList());

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> defaultGetCriteriaOperation.execute(caseTypeDefinition.getId(), CAN_READ, SEARCH));

        assertThat(exception.getMessage(),
            is("CaseField with id=[field1] and path=[null] not found"));
    }

    private WorkbasketInputFieldsDefinition generateWorkbasketInput() {
        WorkbasketInputFieldsDefinition workbasketInputFieldsDefinition = new WorkbasketInputFieldsDefinition();
        workbasketInputFieldsDefinition.setCaseTypeId(caseTypeDefinition.getId());
        workbasketInputFieldsDefinition.setFields(
            asList(getWorkbasketInputField(caseFieldDefinition1.getId(), 1),
                getWorkbasketInputField(caseFieldDefinition2.getId(), 2),
                getWorkbasketInputField(caseFieldDefinition3.getId(), 3),
                getWorkbasketInputField(caseFieldDefinition4.getId(), 4)));
        return workbasketInputFieldsDefinition;
    }

    private WorkbasketInputFieldsDefinition generateWorkbasketInputWithPathElements() {
        String path = PERSON + "." + NAME;
        WorkbasketInputFieldsDefinition workbasketInputFieldsDefinition = new WorkbasketInputFieldsDefinition();
        workbasketInputFieldsDefinition.setCaseTypeId(caseTypeDefinition.getId());
        workbasketInputFieldsDefinition.setFields(asList(
            getWorkbasketInputField(caseFieldDefinition1.getId(), 1),
            getWorkbasketInputField(caseFieldDefinition2.getId(), 2),
            getWorkbasketInputField(caseFieldDefinition3.getId(), 3),
            getWorkbasketInputField(caseFieldDefinition4.getId(), 4),
            getWorkbasketInputField(debtorDetails.getId(), 5, path)
        ));
        return workbasketInputFieldsDefinition;
    }

    private WorkbasketInputField getWorkbasketInputField(String id, int order) {
        return getWorkbasketInputField(id, order, null);
    }

    private WorkbasketInputField getWorkbasketInputField(String id, int order, String path) {
        WorkbasketInputField workbasketInputField = new WorkbasketInputField();
        workbasketInputField.setCaseFieldId(id);
        workbasketInputField.setLabel(id);
        workbasketInputField.setDisplayOrder(order);
        workbasketInputField.setCaseFieldPath(path);
        return workbasketInputField;
    }

    private SearchInputFieldsDefinition generateSearchInputWithPathElements() {
        String path = PERSON + "." + NAME;
        SearchInputFieldsDefinition searchInputFieldsDefinition = new SearchInputFieldsDefinition();
        searchInputFieldsDefinition.setCaseTypeId(caseTypeDefinition.getId());
        searchInputFieldsDefinition.setFields(asList(
            getSearchInputField(caseFieldDefinition1.getId(), 1),
            getSearchInputField(caseFieldDefinition2.getId(), 2),
            getSearchInputField(caseFieldDefinition3.getId(), 3),
            getSearchInputField(caseFieldDefinition4.getId(), 4),
            getSearchInputField(debtorDetails.getId(), 5, path)));
        return searchInputFieldsDefinition;
    }

    private SearchInputFieldsDefinition generateSearchInput() {
        SearchInputFieldsDefinition searchInputFieldsDefinition = new SearchInputFieldsDefinition();
        searchInputFieldsDefinition.setCaseTypeId(caseTypeDefinition.getId());
        searchInputFieldsDefinition.setFields(asList(getSearchInputField(caseFieldDefinition1.getId(), 1),
            getSearchInputField(caseFieldDefinition2.getId(), 2),
            getSearchInputField(caseFieldDefinition3.getId(), 3),
            getSearchInputField(caseFieldDefinition4.getId(), 4)));
        return searchInputFieldsDefinition;

    }

    private SearchInputField getSearchInputField(String id, int order) {
        return getSearchInputField(id, order, null);
    }

    private SearchInputField getSearchInputField(String id, int order, String path) {
        SearchInputField searchInputField = new SearchInputField();
        searchInputField.setCaseFieldId(id);
        searchInputField.setLabel(id);
        searchInputField.setDisplayOrder(order);
        searchInputField.setCaseFieldPath(path);
        searchInputField.setShowCondition(SHOW_CONDITION);
        return searchInputField;
    }
}
