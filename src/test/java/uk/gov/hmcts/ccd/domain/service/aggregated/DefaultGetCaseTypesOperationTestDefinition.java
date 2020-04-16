package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Lists;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

class DefaultGetCaseTypesOperationTestDefinition {

    private static final String JURISDICTION_ID = "TEST";

    @Mock
    private CaseTypeService caseTypeService;

    private CaseTypeDefinition testCaseTypeDefinition1;
    private CaseTypeDefinition testCaseTypeDefinition2;
    private CaseTypeDefinition testCaseTypeDefinition3;
    private List<CaseTypeDefinition> testCaseTypeDefinitions;

    private DefaultGetCaseTypesOperation defaultGetCaseTypesOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        testCaseTypeDefinition1 = newCaseType().build();
        testCaseTypeDefinition2 = newCaseType().build();
        testCaseTypeDefinition3 = newCaseType().build();
        defaultGetCaseTypesOperation = new DefaultGetCaseTypesOperation(caseTypeService);
        testCaseTypeDefinitions = Lists.newArrayList(testCaseTypeDefinition1, testCaseTypeDefinition2, testCaseTypeDefinition3);
        doReturn(testCaseTypeDefinitions).when(caseTypeService).getCaseTypesForJurisdiction(JURISDICTION_ID);
    }

    @Test
    void shouldReturnCaseTypesRegardlessOfAccessParam() {
        List<CaseTypeDefinition> createCaseTypeDefinitions = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_CREATE);
        List<CaseTypeDefinition> readCaseTypeDefinitions = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_READ);
        List<CaseTypeDefinition> updateCaseTypeDefinitions = defaultGetCaseTypesOperation.execute(JURISDICTION_ID, CAN_UPDATE);

        assertAll(
            () -> assertThat(createCaseTypeDefinitions, is(equalTo(readCaseTypeDefinitions))),
            () -> assertThat(readCaseTypeDefinitions, is(equalTo(createCaseTypeDefinitions))),
            () -> assertThat(createCaseTypeDefinitions, is(equalTo(updateCaseTypeDefinitions))),
            () -> assertThat(updateCaseTypeDefinitions, is(equalTo(createCaseTypeDefinitions))),
            () -> assertThat(readCaseTypeDefinitions, is(equalTo(updateCaseTypeDefinitions))),
            () -> assertThat(updateCaseTypeDefinitions, is(equalTo(readCaseTypeDefinitions))),
            () -> assertThat(createCaseTypeDefinitions, IsCollectionContaining.hasItems(testCaseTypeDefinition1,
                                                        testCaseTypeDefinition2, testCaseTypeDefinition3))
        );
    }

}
