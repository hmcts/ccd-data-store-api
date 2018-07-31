package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
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

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class DefaultFindWorkbasketInputOperationTest {
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
        caseType.setCaseFields(Arrays.asList(caseField1, caseField2, caseField3, caseField4));

        findWorkbasketInputOperation = new DefaultFindWorkbasketInputOperation(uiDefinitionRepository, caseDefinitionRepository);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(caseType.getId());
        doReturn(generateWorkbasketInput()).when(uiDefinitionRepository).getWorkbasketInputDefinitions(caseType.getId());
    }

    @Test
    void shouldReturnWorkbasketInputs() {
        List<WorkbasketInput> workbasketInputs = findWorkbasketInputOperation.execute("TEST", caseType.getId(), CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(4)),
            () -> assertThat(workbasketInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(workbasketInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    private WorkbasketInputDefinition generateWorkbasketInput() {
        WorkbasketInputDefinition workbasketInputDefinition = new WorkbasketInputDefinition();
        workbasketInputDefinition.setCaseTypeId(caseType.getId());
        workbasketInputDefinition.setFields(
            Arrays.asList(getField(caseField1.getId(), 1),
                          getField(caseField2.getId(), 2),
                          getField(caseField3.getId(), 3),
                          getField(caseField4.getId(), 4)));
        return workbasketInputDefinition;
    }

    private WorkbasketInputField getField(String id, int order) {
        WorkbasketInputField workbasketInputField = new WorkbasketInputField();
        workbasketInputField.setCaseFieldId(id);
        workbasketInputField.setLabel(id);
        workbasketInputField.setOrder(order);
        return workbasketInputField;
    }
}
