package uk.gov.hmcts.ccd.domain.service.aggregated;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputField;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputField;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("DefaultGetCriteriaOperationTest")
public class DefaultGetCriteriaOperationTest {
    private static final String TEXT_TYPE = "Text";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";

    private final CaseType caseType = new CaseType();
    private final CaseField caseField1 = new CaseField();
    private final CaseField caseField2 = new CaseField();
    private final CaseField caseField3 = new CaseField();
    private final CaseField caseField4 = new CaseField();

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    private DefaultGetCriteriaOperation defaultGetCriteriaOperation;

    private CaseField name = newCaseField().withId(NAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private CaseField surname = newCaseField().withId(SURNAME).withFieldType(aFieldType().withId(TEXT_TYPE).withType(TEXT_TYPE).build()).build();
    private FieldType personFieldType = aFieldType().withId(PERSON).withType(COMPLEX).withComplexField(name).withComplexField(surname).build();
    private CaseField person = newCaseField().withId(PERSON).withFieldType(personFieldType).build();

    private FieldType debtorFieldType = aFieldType().withId(DEBTOR_DETAILS).withType(COMPLEX).withComplexField(person).build();
    private CaseField debtorDetails = newCaseField().withId(DEBTOR_DETAILS).withFieldType(debtorFieldType).build();

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        FieldType fieldType = new FieldType();
        caseField1.setId("field1");
        caseField1.setFieldType(fieldType);
        caseField2.setId("field2");
        caseField2.setFieldType(fieldType);
        caseField3.setId("field3");
        caseField3.setFieldType(fieldType);
        caseField4.setId("field4");
        caseField4.setFieldType(fieldType);
        caseField4.setMetadata(true);
        caseType.setId("Test case type");
        caseType.setCaseFields(asList(caseField1, caseField2, caseField3, caseField4, debtorDetails));

        defaultGetCriteriaOperation = new DefaultGetCriteriaOperation(uiDefinitionRepository, caseDefinitionRepository);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(caseType.getId());
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
        doReturn(generateSearchInput()).when(uiDefinitionRepository).getSearchInputDefinitions(caseType.getId());
    }

    @Test
    void shouldReturnWorkbasketInputs() {
        List<? extends CriteriaInput> workbasketInputs = defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(workbasketInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    @Test
    void shouldReturnSearchInputs() {
        List<? extends CriteriaInput> searchInputs = defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(4)),
            () -> assertThat(searchInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(searchInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    @Test
    void shouldReturnWorkbasketInputsWhenCaseFieldElementPathDefined() {
        doReturn(generateWorkbasketInputWithPathElements()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
        List<? extends CriteriaInput> workbasketInputs = defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(5)),
            () -> assertThat(workbasketInputs.get(4).getField().getId(), is(DEBTOR_DETAILS)),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getType(), is(name.getFieldType().getType())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getId(), is(name.getFieldType().getId())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getChildren().size(), is(0))
        );
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCaseFieldNotFoundInCaseTypeForWorkbasketInput() {
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
        caseType.setCaseFields(Collections.emptyList());

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, WORKBASKET));

        assertThat(exception.getMessage(),
            is("CaseField with id=[field1] and path=[null] not found"));
    }

    @Test
    void shouldReturnSearchInputsWhenCaseFieldElementPathDefined() {
        doReturn(generateSearchInputWithPathElements()).when(uiDefinitionRepository).getSearchInputDefinitions(caseType.getId());
        List<? extends CriteriaInput> searchInputs = defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(5)),
            () -> assertThat(searchInputs.get(4).getField().getId(), is(DEBTOR_DETAILS)),
            () -> assertThat(searchInputs.get(4).getField().getType().getType(), is(name.getFieldType().getType())),
            () -> assertThat(searchInputs.get(4).getField().getType().getId(), is(name.getFieldType().getId())),
            () -> assertThat(searchInputs.get(4).getField().getType().getChildren().size(), is(0)));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenCaseFieldNotFoundInCaseTypeForSearchInput() {
        doReturn(generateSearchInput()).when(uiDefinitionRepository).getSearchInputDefinitions(caseType.getId());
        caseType.setCaseFields(Collections.emptyList());

        final BadRequestException exception = assertThrows(BadRequestException.class,
            () -> defaultGetCriteriaOperation.execute(caseType.getId(), CAN_READ, SEARCH));

        assertThat(exception.getMessage(),
            is("CaseField with id=[field1] and path=[null] not found"));
    }

    private WorkbasketInputDefinition generateWorkbasketInput() {
        WorkbasketInputDefinition workbasketInputDefinition = new WorkbasketInputDefinition();
        workbasketInputDefinition.setCaseTypeId(caseType.getId());
        workbasketInputDefinition.setFields(
            asList(getWorkbasketInputField(caseField1.getId(), 1),
                getWorkbasketInputField(caseField2.getId(), 2),
                getWorkbasketInputField(caseField3.getId(), 3),
                getWorkbasketInputField(caseField4.getId(), 4)));
        return workbasketInputDefinition;
    }

    private WorkbasketInputDefinition generateWorkbasketInputWithPathElements() {
        String path = PERSON + "." + NAME;
        WorkbasketInputDefinition workbasketInputDefinition = new WorkbasketInputDefinition();
        workbasketInputDefinition.setCaseTypeId(caseType.getId());
        workbasketInputDefinition.setFields(asList(
            getWorkbasketInputField(caseField1.getId(), 1),
            getWorkbasketInputField(caseField2.getId(), 2),
            getWorkbasketInputField(caseField3.getId(), 3),
            getWorkbasketInputField(caseField4.getId(), 4),
            getWorkbasketInputField(debtorDetails.getId(), 5, path)
        ));
        return workbasketInputDefinition;
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

    private SearchInputDefinition generateSearchInputWithPathElements() {
        String path = PERSON + "." + NAME;
        SearchInputDefinition searchInputDefinition = new SearchInputDefinition();
        searchInputDefinition.setCaseTypeId(caseType.getId());
        searchInputDefinition.setFields(asList(
            getSearchInputField(caseField1.getId(), 1),
            getSearchInputField(caseField2.getId(), 2),
            getSearchInputField(caseField3.getId(), 3),
            getSearchInputField(caseField4.getId(), 4),
            getSearchInputField(debtorDetails.getId(), 5, path)));
        return searchInputDefinition;
    }

    private SearchInputDefinition generateSearchInput() {
        SearchInputDefinition searchInputDefinition = new SearchInputDefinition();
        searchInputDefinition.setCaseTypeId(caseType.getId());
        searchInputDefinition.setFields(asList(getSearchInputField(caseField1.getId(), 1),
            getSearchInputField(caseField2.getId(), 2),
            getSearchInputField(caseField3.getId(), 3),
            getSearchInputField(caseField4.getId(), 4)));
        return searchInputDefinition;

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
        return searchInputField;
    }
}
