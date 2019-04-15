package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputField;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

@DisplayName("DefaultFindWorkbasketInputOperation")
class DefaultFindWorkbasketInputOperationTest {
    private static final String TEXT_TYPE = "Text";
    private static final String PERSON = "Person";
    private static final String DEBTOR_DETAILS = "Debtor details";
    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private final CaseType caseType = new CaseType();
    private DefaultFindWorkbasketInputOperation findWorkbasketInputOperation;
    private final CaseField caseField1 = new CaseField();
    private final CaseField caseField2 = new CaseField();
    private final CaseField caseField3 = new CaseField();
    private final CaseField caseField4 = new CaseField();

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

        findWorkbasketInputOperation = new DefaultFindWorkbasketInputOperation(uiDefinitionRepository, caseDefinitionRepository);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(caseType.getId());
    }

    @Test
    void shouldReturnWorkbasketInputs() {
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
        List<WorkbasketInput> workbasketInputs = findWorkbasketInputOperation.execute(caseType.getId(), CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(workbasketInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    @Test
    void shouldReturnWorkbasketInputsWhenCaseFieldElementPathDefined() {
        doReturn(generateWorkbasketInputWithPathElements()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
        List<WorkbasketInput> workbasketInputs = findWorkbasketInputOperation.execute(caseType.getId(), CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(5)),
            () -> assertThat(workbasketInputs.get(4).getField().getId(), is(DEBTOR_DETAILS)),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getType(), is(name.getFieldType().getType())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getId(), is(name.getFieldType().getId())),
            () -> assertThat(workbasketInputs.get(4).getField().getType().getChildren().size(), is(0))
        );
    }

    private WorkbasketInputDefinition generateWorkbasketInput() {
        WorkbasketInputDefinition workbasketInputDefinition = new WorkbasketInputDefinition();
        workbasketInputDefinition.setCaseTypeId(caseType.getId());
        workbasketInputDefinition.setFields(asList(
                getField(caseField1.getId(), 1),
                getField(caseField2.getId(), 2),
                getField(caseField3.getId(), 3),
                getField(caseField4.getId(), 4)));
        return workbasketInputDefinition;
    }

    private WorkbasketInputDefinition generateWorkbasketInputWithPathElements() {
        String path = PERSON + "." + NAME;
        WorkbasketInputDefinition workbasketInputDefinition = new WorkbasketInputDefinition();
        workbasketInputDefinition.setCaseTypeId(caseType.getId());
        workbasketInputDefinition.setFields(asList(
                getField(caseField1.getId(), 1),
                getField(caseField2.getId(), 2),
                getField(caseField3.getId(), 3),
                getField(caseField4.getId(), 4),
                getField(debtorDetails.getId(), 5, path)
            ));
        return workbasketInputDefinition;
    }

    private WorkbasketInputField getField(String id, int order, String path) {
        WorkbasketInputField workbasketInputField = new WorkbasketInputField();
        workbasketInputField.setCaseFieldId(id);
        workbasketInputField.setLabel(id);
        workbasketInputField.setOrder(order);
        workbasketInputField.setCaseFieldElementPath(path);
        return workbasketInputField;
    }

    private WorkbasketInputField getField(String id, int order) {
        return getField(id, order, null);
    }
}
