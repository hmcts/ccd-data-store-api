package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.aCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

class AuthorisedFindWorkbasketInputOperationTest {
    private static final String JURISDICTION_ID = "TEST";
    private static final String CASE_TYPE_ONE = "CaseTypeOne";
    private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
    private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
    private static final String CASE_FIELD_ID_1_3 = "CASE_FIELD_1_3";
    private static final String CASE_FIELD_ID_1_4 = "CASE_FIELD_1_4";
    private static final CaseField CASE_FIELD_1_1 = aCaseField().withId(CASE_FIELD_ID_1_1).build();
    private static final CaseField CASE_FIELD_1_2 = aCaseField().withId(CASE_FIELD_ID_1_2).build();
    private static final CaseField CASE_FIELD_1_3 = aCaseField().withId(CASE_FIELD_ID_1_3).build();
    private static List<WorkbasketInput> testWorkbasketInputs;
    @Mock
    private FindWorkbasketInputOperation findWorkbasketInputOperation;
    @Mock
    private GetCaseTypesOperation getCaseTypesOperation;
    private AuthorisedFindWorkbasketInputOperation authorisedFindWorkbasketInputOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        testWorkbasketInputs = Arrays.asList(
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_1).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_2).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_3).build(),
            aWorkbasketInput().withFieldId(CASE_FIELD_ID_1_4).build()
        );
        CaseType testCaseType = newCaseType()
            .withField(CASE_FIELD_1_1)
            .withField(CASE_FIELD_1_2)
            .withField(CASE_FIELD_1_3)
            .build();
        testCaseType.setId(CASE_TYPE_ONE);
        List<CaseType> testCaseTypes = Lists.newArrayList(testCaseType);

        doReturn(testCaseTypes).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);

        authorisedFindWorkbasketInputOperation = new AuthorisedFindWorkbasketInputOperation
            (findWorkbasketInputOperation, getCaseTypesOperation);
    }


    @Test
    @DisplayName("should fail when no case type due to no READ access type")
    void shouldFailWhenWhenNoReadAccess() {
        doReturn(Collections.EMPTY_LIST).when(getCaseTypesOperation).execute(JURISDICTION_ID, CAN_READ);

        assertThrows(ResourceNotFoundException.class, () -> authorisedFindWorkbasketInputOperation.execute
            (JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ));
    }

    @Test
    @DisplayName("should return only authorised case fields")
    void shouldReturnOnlyAuthorisedCaseFields() {
        doReturn(testWorkbasketInputs).when(findWorkbasketInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        final List<WorkbasketInput> workbasketInputs = authorisedFindWorkbasketInputOperation.execute
            (JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(3)),
            () -> assertThat(workbasketInputs.get(0), is(testWorkbasketInputs.get(0))),
            () -> assertThat(workbasketInputs.get(1), is(testWorkbasketInputs.get(1))),
            () -> assertThat(workbasketInputs.get(2), is(testWorkbasketInputs.get(2)))
        );
    }

    @Test
    @DisplayName("should return empty workbasket input list when no field is authorised")
    void shouldReturnEmptyWorkbasketInputWhenNoFieldIsAuthorised() {
        doReturn(new ArrayList<>()).when(findWorkbasketInputOperation).execute(JURISDICTION_ID, CASE_TYPE_ONE,
            CAN_READ);

        final List<WorkbasketInput> workbasketInputs = authorisedFindWorkbasketInputOperation.execute
            (JURISDICTION_ID, CASE_TYPE_ONE, CAN_READ);

        assertAll(
            () -> assertThat(workbasketInputs.size(), is(0))
        );
    }
}
