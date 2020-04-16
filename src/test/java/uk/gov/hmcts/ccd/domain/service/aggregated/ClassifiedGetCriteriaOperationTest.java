package uk.gov.hmcts.ccd.domain.service.aggregated;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ClassifiedGetCriteriaOperationTest {
    private static final String JURISDICTION_ID = "TEST";
    private static final String CASE_TYPE_ONE = "CaseTypeOne";
    private static final String PUBLIC = "PUBLIC";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_1_4 = "CASE_FIELD_1_4";
    private static final CaseFieldDefinition CASE_FIELD_1_1 = newCaseField().withId(CASE_FIELD_ID_1_1).withSC(PUBLIC).build();
    private static final CaseFieldDefinition CASE_FIELD_1_2 = newCaseField().withId(CASE_FIELD_ID_1_2).withSC(PUBLIC).build();
    private static final CaseFieldDefinition CASE_FIELD_1_3 = newCaseField().withId(CASE_FIELD_ID_1_3).withSC(PUBLIC).build();
    private static final CaseFieldDefinition CASE_FIELD_1_4 = newCaseField().withId(CASE_FIELD_ID_1_4).withSC(PUBLIC).build();
    private static List<WorkbasketInput> testWorkbasketInputs;
    private static List<SearchInput> testSearchInputs;


    @Mock
    private GetCriteriaOperation getCriteriaOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedGetCriteriaOperation classUnderTest;
    private CaseTypeDefinition testCaseTypeDefinition;

    @BeforeEach
    void setUp() {
        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_4).build()
        );
        testSearchInputs = Arrays.asList(aSearchInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_4).build()
        );

        MockitoAnnotations.initMocks(this);
        testCaseTypeDefinition = newCaseType()
            .withJurisdiction(newJurisdiction()
                                  .withJurisdictionId(JURISDICTION_ID)
                                  .build())
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withField(CASE_FIELD_1_4)
            .build();
        testCaseTypeDefinition.setId(CASE_TYPE_ONE);

        doReturn(testCaseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ONE);

        classUnderTest = new ClassifiedGetCriteriaOperation(getCriteriaOperation, caseDefinitionRepository, classificationService);

    }

    @Test
    @DisplayName("should filter workbasket input fields when user does not have enough SC rank")
    void shouldFilterFieldsWithSC() {
        doReturn(testWorkbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE,
            CAN_READ, WORKBASKET);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_1);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_2);
        doReturn(false).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_3);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_4);


        final List<? extends CriteriaInput> workbasketInputs = classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);
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
        doReturn(new ArrayList<>()).when(getCriteriaOperation).execute(CASE_TYPE_ONE,
            CAN_READ, WORKBASKET);

        final List<? extends CriteriaInput> workbasketInputs = classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, WORKBASKET);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(0))
        );
    }

    @Test
    @DisplayName("should filter search input fields when user does not have enough SC rank")
    void shouldFilterSearchInputFieldsWithSC() {
        doReturn(testSearchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_1);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_2);
        doReturn(false).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_3);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseTypeDefinition, CASE_FIELD_ID_1_4);


        final List<? extends CriteriaInput> searchInputs = classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);
        assertAll(
            () -> assertThat(searchInputs.size(), is(3)),
            () -> assertThat(searchInputs.get(0), is(testSearchInputs.get(0))),
            () -> assertThat(searchInputs.get(1), is(testSearchInputs.get(1))),
            () -> assertThat(searchInputs.get(2), is(testSearchInputs.get(3)))
        );
    }

    @Test
    @DisplayName("should return empty search input list when user does't have enough SC for any field")
    void shouldReturnEmptySearchInputWhenNoFieldIsAuthorised() {
        doReturn(new ArrayList<>()).when(getCriteriaOperation).execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        final List<? extends CriteriaInput> searchInputs = classUnderTest.execute(CASE_TYPE_ONE, CAN_READ, SEARCH);

        assertAll(
            () -> assertThat(searchInputs.size(), is(0))
        );
    }
}
