package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

class DefaultGetCaseTypesOperationTest {

    private static final String JURISDICTION_ID = "TEST";

    @Mock
    private CaseTypeService caseTypeService;

    private CaseType testCaseType1;
    private CaseType testCaseType2;
    private CaseType testCaseType3;
    private List<CaseType> testCaseTypes;

    private DefaultGetCaseTypesOperation defaultGetCaseTypesOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        testCaseType1 = newCaseType().build();
        testCaseType2 = newCaseType().build();
        testCaseType3 = newCaseType().build();
        defaultGetCaseTypesOperation = new DefaultGetCaseTypesOperation(caseTypeService);
        testCaseTypes = Lists.newArrayList(testCaseType1, testCaseType2, testCaseType3);
        doReturn(testCaseTypes).when(caseTypeService).getCaseTypesForJurisdiction(JURISDICTION_ID);
    }

    @Test
    void shouldReturnCaseTypesRegardlessOfAccessParam() {
        List<CaseType> createCaseTypes = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_CREATE);
        List<CaseType> readCaseTypes = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);
        List<CaseType> updateCaseTypes = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_UPDATE);

        assertAll(
            () -> assertThat(createCaseTypes, is(equalTo(readCaseTypes))),
            () -> assertThat(readCaseTypes, is(equalTo(createCaseTypes))),
            () -> assertThat(createCaseTypes, is(equalTo(updateCaseTypes))),
            () -> assertThat(updateCaseTypes, is(equalTo(createCaseTypes))),
            () -> assertThat(readCaseTypes, is(equalTo(updateCaseTypes))),
            () -> assertThat(updateCaseTypes, is(equalTo(readCaseTypes))),
            () -> assertThat(createCaseTypes, IsCollectionContaining.hasItems(testCaseType1, testCaseType2, testCaseType3))
        );
    }

}
