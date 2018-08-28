package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
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
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;

class ClassifiedFindSearchInputOperationTest {
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
    private static List<SearchInput> testSearchInputs;

    @Mock
    private FindSearchInputOperation findSearchInputOperation;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private SecurityClassificationService classificationService;

    private ClassifiedFindSearchInputOperation classUnderTest;
    private CaseType testCaseType;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.initMocks(this);
        testSearchInputs = Arrays.asList(aSearchInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aSearchInput().withFieldId(CASE_FIELD_ID_1_4).build()
        );
        testCaseType = newCaseType()
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .withField(CASE_FIELD_1_4)
            .build();
        testCaseType.setId(CASE_TYPE_ONE);

        doReturn(testCaseType).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ONE);

        classUnderTest = new ClassifiedFindSearchInputOperation(findSearchInputOperation, caseDefinitionRepository,
            classificationService);

    }

    @Test
    @DisplayName("should filter search input fields when user does not have enough SC rank")
    void shouldFilterSearchInputFieldsWithSC() {
        doReturn(testSearchInputs).when(findSearchInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_1);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_2);
        doReturn(false).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_3);
        doReturn(true).when(classificationService).userHasEnoughSecurityClassificationForField(JURISDICTION_ID,
            testCaseType, CASE_FIELD_ID_1_4);


        final List<SearchInput> searchInputs = classUnderTest.execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);
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
        doReturn(new ArrayList<>()).when(findSearchInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        final List<SearchInput> searchInputs = classUnderTest.execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        assertAll(
            () -> assertThat(searchInputs.size(), is(0))
        );
    }

}
