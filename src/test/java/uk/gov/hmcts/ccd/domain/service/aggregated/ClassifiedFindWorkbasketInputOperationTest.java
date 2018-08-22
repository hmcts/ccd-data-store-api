package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

class ClassifiedFindWorkbasketInputOperationTest {
    private static final String JURISDICTION_ID = "TEST";
    private static final String CASE_TYPE_ONE = "CaseTypeOne";
    private static final String PUBLIC = "PUBLIC";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_1_4 = "CASE_FIELD_1_4";
    private static final CaseField CASE_FIELD_1_1 = aCaseField().withId(CASE_FIELD_ID_1_1).withSC(PUBLIC).build();
    private static final CaseField CASE_FIELD_1_2 = aCaseField().withId(CASE_FIELD_ID_1_2).withSC(PUBLIC).build();
    private static final CaseField CASE_FIELD_1_3 = aCaseField().withId(CASE_FIELD_ID_1_3).withSC(PUBLIC).build();
    private static final CaseField CASE_FIELD_1_4 = aCaseField().withId(CASE_FIELD_ID_1_4).withSC(PUBLIC).build();
    private static List<WorkbasketInput> testWorkbasketInputs;

    @Mock
    private FindWorkbasketInputOperation findWorkbasketInputOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedFindWorkbasketInputOperation classUnderTest;
    private CaseType testCaseType;

    @BeforeEach
    void setUp() {
        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_4).build()
        );

        MockitoAnnotations.initMocks(this);
        testCaseType = newCaseType()
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withField(CASE_FIELD_1_4)
            .build();
        testCaseType.setId(CASE_TYPE_ONE);

        doReturn(testCaseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ONE);

        classUnderTest = new ClassifiedFindWorkbasketInputOperation(findWorkbasketInputOperation,
            caseDefinitionRepository, classificationService);

    }

    @Test
    @DisplayName("should filter workbasket input fields when user does not have enough SC rank")
    void shouldFilterFieldsWithSC() {
        doReturn(testWorkbasketInputs).when(findWorkbasketInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE,
            CAN_READ);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_1);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_2);
        doReturn(false).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_3);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_4);


        final List<WorkbasketInput> workbasketInputs = classUnderTest.execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);
        assertAll(
            () -> assertThat(workbasketInputs.size(), is(3)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(3)))
        );
    }

    @Test
    @DisplayName("should return empty workbasket input list when user does't have enough SC for any field")
    void shouldReturnEmptyWorkbasketInputWhenNoFieldIsAuthorised() {
        doReturn(new ArrayList<>()).when(findWorkbasketInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE,
            CAN_READ);

        final List<WorkbasketInput> workbasketInputs = classUnderTest.execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(0))
        );
    }
}
